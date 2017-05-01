/*
 * Copyright 2017 Alex Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.unclealex.devsync

import java.io.ByteArrayOutputStream
import java.net.URL

import android.content.{DialogInterface, Intent}
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.provider.DocumentFile
import android.support.v7.app.{AlertDialog, AppCompatActivity}
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.view.{Gravity, LayoutInflater, View, ViewGroup}
import android.widget._
import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import devsync.json._
import devsync.logging.Messages
import devsync.remote.ChangesClient
import macroid.FullDsl.{text, _}
import macroid._
import macroid.extras.ImageViewTweaks._
import uk.co.unclealex.devsync.Async._
import uk.co.unclealex.devsync.DocumentFileResource._
import uk.co.unclealex.devsync.IntentHelper._

import scala.concurrent.Future


/**
  * The activity that lists the changes since the device was updated and offers a button to synchronise.
  **/
class DeviceSynchroniserActivity extends AppCompatActivity with Contexts[AppCompatActivity] with StrictLogging {

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.device_synchroniser_activity)
  }


  override def onResume(): Unit = {
    super.onResume()
    implicit val resourceStreamProvider: DocumentFileResourceStreamProvider = new DocumentFileResourceStreamProvider()
    val intent = getIntent
    val resourceUri = intent.resourceUri
    Services.device.reloadDeviceDescriptor(DocumentFile.fromTreeUri(this, Uri.parse(resourceUri))) match {
      case Right(deviceDescriptor) =>
        if (deviceDescriptor != intent.deviceDescriptor) {
          intent.putDeviceDescriptor(deviceDescriptor)
          val serverUrl = intent.serverUrl
          val rootView = findViewById(R.id.synchronise_content)
          def setupGui(): Ui[_] = {
            Transformer {
              case LastUpdatedView(t) => t <~ text(Messages.Changes.maybeLastUpdated(deviceDescriptor.maybeLastModified))
              case ChangeCountView(t) => t <~ text(Messages.Changes.changes(0))
              case SynchroniseButtonView(b) => b <~ disable <~ On.click {
                val synchroniseIntent =
                  new Intent(this, classOf[SynchroniseService]).
                    putDeviceDescriptor(deviceDescriptor).
                    putResourceUri(resourceUri).
                    putServerUrl(serverUrl)
                this.startService(synchroniseIntent)
                toast(Messages.Changes.synchronisationStarted) <~ gravity(Gravity.CENTER, xOffset = 3.dp) <~ fry
              }
            }(rootView)
          }
          def updateChangelog(): Future[_] = {
            val changesClient = Services.changesClient(new URL(serverUrl))
            populateChangelog(changesClient, deviceDescriptor.user, deviceDescriptor.maybeLastModified).mapUi {
              case Right(changelog) =>
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
                  case SynchroniseButtonView(b) =>
                    if (changelog.items.isEmpty) b <~ disable <~ hide else b <~ enable <~ show
                }(rootView)
              case Left(e) =>
                logger.error("Could not get the list of changes.", e)
                dialog(e.getMessage)
            }
          }
          setupGui().run.flatMap(_ => updateChangelog())
        }
      case Left(_) =>
        val builder = new AlertDialog.Builder(this)
        builder.setMessage(R.string.no_device_descriptor_message).setTitle(R.string.no_device_descriptor_title)
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          def onClick(dialog: DialogInterface , id: Int) {
            finishAndRemoveTask()
          }
        })
        val dialog = builder.create
        dialog.show()
    }
  }

  /**
    * A recycler view adaptor to show changelog items.
 *
    * @param changesClient The [[ChangesClient]] used to get changes.
    * @param changelog The changelog for this device.
    */
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

  /**
    * A view to show album information.
    * @param view The parent view.
    */
  class AlbumViewHolder(val view: View) extends RecyclerView.ViewHolder(view) {
    val albumArtwork: Ui[Option[ImageView]] = view.find[ImageView](R.id.album_cover_view)
    val albumText: Ui[Option[TextView]] = view.find[TextView](R.id.album_text_view)
    val artistText: Ui[Option[TextView]] = view.find[TextView](R.id.artist_text_view)
    val timeText: Ui[Option[TextView]] = view.find[TextView](R.id.time_text_view)
  }

  def populateChangelog(
                         changesClient: ChangesClient,
                         user: String,
                         maybeLastModified: Option[IsoDate]): Future[Either[Exception, Changelog]] = Future {
    changesClient.changelogSince(user, maybeLastModified)
  }

  /**
    * An extractor for the change count view.
    */
  object ChangeCountView extends ViewExtractor[TextView](R.id.changeCountView)

  /**
    * An extractor for the last updated view.
    */
  object LastUpdatedView extends ViewExtractor[TextView](R.id.lastUpdatedView)

  /**
    * An extractor for the synchronise button.
    */
  object SynchroniseButtonView extends ViewExtractor[FloatingActionButton](R.id.fab_action_button)

  /**
    * An extractor for the changes container.
    */
  object ChangesContainerView extends ViewExtractor[RecyclerView](R.id.recycler)
}
