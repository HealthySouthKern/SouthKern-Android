<h1 align="center"> Building Healthy Communities App </h1> <br>
<p align="center">
  <a href="https://www.healthysouthkern.org">
    <img alt="BHC" title="BHC" src="https://docs.google.com/uc?id=0Bxpf6hnE1MxYZlQxbHIweVJLMkNjYlpnX2l1YkljMkRhN0RN" width="250">
  </a>
</p>

<p align="center">
  BHC in your pocket. Built with Firebase.
</p>
  <p align="center">
  Coming Soon iOS!
</p>

<p align="center">
  <a href="">
    <img alt="Download on the App Store" title="App Store" src="http://i.imgur.com/0n2zqHD.png" width="140">
</a>

 <a href="https://play.google.com/store/apps/details?id=com.eddierangel.southkern.android&ah=wpZEbn4e8TKbMdNAnc8bSIzzd5w">
    <img alt="Get it on Google Play" title="Google Play" src="http://i.imgur.com/mtGRPuM.png" width="140">
  </a>
</p>

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Feedback](#feedback)
- [Contributors](#contributors)
- [Build Process](#build-process)
- [Backers](#backers-)
- [Sponsors](#sponsors-)
- [Acknowledgments](#acknowledgments)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Introduction

**Available for Android.**

<p align="center">
  <img src ="https://docs.google.com/uc?id=1FKWDZoTaWq7YVb3mA6k8zGCIT6teSmgP" width=350>
</p>

## Features

A few of the things you can do with BHC:

* Sign in with Google
* View activity feed
* Create your own group channel with others
* Join an open channel
* Update and delete reminders
* Look over calendar feed
* Edit your profile
* Automatic notifications
* Easily search for contacts

<p align="center">
  <img src = "https://docs.google.com/uc?id=1ZTLod1L3AS3Sr4Egk0sm4ONv0JA704xy" width=350>
</p>

<p align="center">
  <img src = "https://docs.google.com/uc?id=1KQAVU-2NwlVRUGKix_vbs1A1EisCnmEg" width=350>
</p>

## Feedback

Feel free to send us feedback on [Twitter](https://twitter.com/bhcsouthkern) or [file an issue](info@healthysouthkern.org). Feature requests are always welcome.

If there's anything you'd like to chat about, please feel free to email [BHC group](edward.rangel@bakersfieldcollege.edu)!

## Contributors

This project is brought to you by these incredible contributors:

* Eddie Rangel
* James Schmiechen
* Noah West
* Alan Marin
* Diana Balderas

## Build Process

- Follow the [SendBird Guide](https://facebook.github.io/react-native/docs/getting-started.html) for getting started building a project with native code. **A Mac is required if you wish to develop for iOS.**

You can also open the sample project from **Android Studio**.

Build and run the Sample UI project to play around with Open Channels and Group Channels.

> The sample project is shipped with a **Testing App ID**. This means that you are sharing the app, including its users, channels, and messages, with everyone who downloads this sample or samples in other platforms. To use the sample in your own app, see the **2. Integrating the sample into your own app** section.

### Notes

* This sample currently uses `v25.3.0` of the Android Support Libraries.
* If you encounter a `Failed to resolve: com.google.firebase:firebase-messaging:9.6.1` error message while building, please install or upgrade to the latest Google Repository from the SDK Manager.
* The current minimum SDK version is `14`. This is due to the Google Play Services and Firebase dropping support for `<14` versions beginning from Google Play Services `v10.2.0`.

    However, the SendBird SDK is compatible with all Android versions from Gingerbread(SDK version 10), and if you wish to run the sample on an older device, you can simply downgrade the Firebase version.


## Integrating the sample into your own app

If you wish to use parts of the sample for messaging in your own app, you must create a new SendBird application from the **[SendBird Dashboard](https://dashboard.sendbird.com)**. If you do not yet have an account, you can log in with Google, GitHub, or create a new account.

**Development Keys**: After you create a SendBird application in the Dashboard, replace `APP_ID` in `BaseApplication` with your own App ID. You will then be able to manage the users and channels, as well as general settings of your messaging app, through your Dashboard.

> All users within the same SendBird application are able to communicate with each other, across all platforms. This means users using iOS, Android, web clients, etc. can all chat with one another. However, users in different SendBird applications cannot talk to each other.

## Acknowledgments

Thanks to everyone who made an effort on building this amazing project for the community.
