# Device Synchroniser

The sister project to [Flac Manager](https://github.com/unclealex72/flac-manager), this project
lets you synchronise either a USB stick or an Android device with music on a Flac Manager
server.

A device must have a special file called `device.json` which, initially must contain the following:

```
  {
    "user": "<username>"
  }
```

where `<username>` is the name of the Flac Manager user who owns the device.

## Applications

There is an Android application that gets packaged as an `.apk` and a 
[ScalaFX](http://www.scalafx.org/) desktop application that gets packaged as a `.deb`.