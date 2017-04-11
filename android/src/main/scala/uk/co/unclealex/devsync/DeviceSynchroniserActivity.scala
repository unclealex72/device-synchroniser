package uk.co.unclealex.devsync

import java.io.ByteArrayOutputStream
import java.net.URL
import java.text.SimpleDateFormat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.view.ViewGroup.LayoutParams
import android.view.{Gravity, LayoutInflater, View, ViewGroup}
import android.widget._
import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import devsync.common.Messages
import devsync.json.{Changelog, ChangelogItem, IsoDate, RelativePath}
import devsync.remote.ChangesClient
import macroid.FullDsl.{text, _}
import macroid._
import macroid.extras.ImageViewTweaks._
import macroid.extras.FloatingActionButtonTweaks._
import uk.co.unclealex.devsync.Async._
import uk.co.unclealex.devsync.IntentHelper._

import scala.concurrent.Future


/**
  * Created by alex on 30/03/17
  **/
class DeviceSynchroniserActivity extends AppCompatActivity with Contexts[AppCompatActivity] with StrictLogging {

  override def onCreate(savedInstanceState: Bundle): Unit = {
    val intent = getIntent
    val deviceDescriptor = intent.deviceDescriptor
    val resourceUri = intent.resourceUri
    val serverUrl = intent.serverUrl
    super.onCreate(savedInstanceState)
    setContentView(R.layout.device_synchroniser_activity)
    val rootView = findViewById(R.id.content)
    def setupGui(): Ui[_] = {
      Transformer {
        case LastUpdatedView(t) => t <~ text(Messages.Changes.maybeLastUpdated(deviceDescriptor.maybeLastModified))
        case ChangeCountView(t) => t <~ text(Messages.Changes.changes(0))
        case SynchroniseButtonView(b) => b <~ disable <~ On.click {
          val synchroniseIntent = new Intent(this, classOf[SynchroniseService])
          synchroniseIntent.put(deviceDescriptor, resourceUri, serverUrl)
          this.startService(synchroniseIntent)
          toast(Messages.Changes.synchronisationStarted) <~ gravity(Gravity.CENTER, xOffset = 3.dp) <~ fry
        }
      }(rootView)
    }
    def updateChangelog(): Future[_] = {
      val changesClient = Services.changesClient(new URL(serverUrl))
      populateChangelog(changesClient, deviceDescriptor.user, deviceDescriptor.maybeLastModified).mapUi {
        case Right(changelog) if changelog.items.nonEmpty =>
          Transformer {
            case ChangesContainerView(rv) =>
              rv.setLayoutManager {
                val llm = new LinearLayoutManager(this)
                llm.setOrientation(LinearLayoutManager.VERTICAL)
                llm
              }
              rv.setHasFixedSize(true)
              rv.setAdapter(new ChangelogItemAdapter(changesClient, changelog))
              rv <~ show
            case ChangeCountView(t) => t <~ text(Messages.Changes.changes(changelog.items.size))
            case SynchroniseButtonView(b) => b <~ enable
          }(rootView)
        case Right(changelog) if changelog.items.isEmpty =>
          dialog("Empty")
        case Left(e) =>
          logger.error("Could not get the list of changes.", e)
          dialog(e.getMessage)
      }
    }
    setupGui().run.flatMap(_ => updateChangelog())
  }


  class ChangelogItemAdapter(changesClient: ChangesClient, changelog: Changelog) extends RecyclerView.Adapter[AlbumViewHolder] {
    override def getItemCount: Int = changelog.items.size

    override def onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): AlbumViewHolder = {
      val itemView = LayoutInflater.
        from(viewGroup.getContext).
        inflate(R.layout.changelog_item, viewGroup, false)
      new AlbumViewHolder(itemView)
    }

    private def loadArtwork(vh: AlbumViewHolder, changelogItem: ChangelogItem) = Future {
      val artworkView = vh.albumArtwork.get.get
      val maybeBuffer = {
        val buffer = new ByteArrayOutputStream()
        changesClient.artwork(changelogItem, buffer).toOption.map(_ => buffer.toByteArray)
      }
      Artwork(maybeBuffer, artworkView.getMaxWidth, artworkView.getMaxHeight)
    }.mapUi { bitmap =>
      vh.albumArtwork <~ ivSrc(bitmap)
    }

    private def loadTags(vh: AlbumViewHolder, changelogItem: ChangelogItem) = Future {
      changesClient.tags(changelogItem)
    }.map {
      case Right(tags) =>
        (tags.album, tags.artist)
      case Left(_) =>
        (changelogItem.relativePath.toString, Messages.Changes.removed)
    }.mapUi {
      case (albumText, artistText) =>
        Ui.sequence(
          vh.albumText <~ text(albumText),
          vh.artistText <~ text(artistText)
        )

    }

    override def onBindViewHolder(vh: AlbumViewHolder, idx: Int): Unit = {
      if (changelog.items.isDefinedAt(idx)) {
        val changelogItem = changelog.items(idx)
        loadArtwork(vh, changelogItem)
        loadTags(vh, changelogItem)
        Ui.run(vh.timeText <~ text(changelogItem.at.format("dd/MM/yyyy HH:mm:ss")))
      }
    }


  }

  class AlbumViewHolder(val view: View) extends RecyclerView.ViewHolder(view) {
    val albumArtwork: Ui[Option[ImageView]] = view.find[ImageView](R.id.album_cover_view)
    val albumText: Ui[Option[TextView]] = view.find[TextView](R.id.album_text_view)
    val artistText: Ui[Option[TextView]] = view.find[TextView](R.id.artist_text_view)
    val timeText: Ui[Option[TextView]] = view.find[TextView](R.id.time_text_view)
  }

  def populateChangelog(changesClient: ChangesClient, user: String, maybeLastModified: Option[IsoDate]): Future[Either[Exception, Changelog]] = Future {
    changesClient.changelogSince(user, maybeLastModified)
  }

  // Extractors for the different view components
  class ViewExtractor[V <: View](val id: Int) {
    def unapply(view: View): Option[V] = view match {
      case v: V if v.getId == id => Some(v)
      case _ => None
    }
  }
  object ChangeCountView extends ViewExtractor[TextView](R.id.changeCountView)
  object LastUpdatedView extends ViewExtractor[TextView](R.id.lastUpdatedView)
  object SynchroniseButtonView extends ViewExtractor[FloatingActionButton](R.id.fab_action_button)
  object ChangesContainerView extends ViewExtractor[RecyclerView](R.id.recycler)
}
