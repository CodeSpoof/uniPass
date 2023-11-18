# uniPass
A password manager and changer for the univention corporate server software

[![swift-version](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/io.codespoof.univpassm )](https://apt.izzysoft.de/packages/io.codespoof.univpassm/)

To avoid tedious tasks like regularly changing one's password, this app allows the user to change it directly on their mobile phone with one click. This results not only in less effort for the individual user, but also in more secure passwords. These are in fact randomly generated according to the criteria set by the UCS.

## Features
The app supports
- Changing the password with the click of a button
- Showing the history of set passwords
- Changing the password generation
  - By length
  - To only contain characters which can be used in a url without escaping
- Protection of saved data [-> Security and Limitations](#security-and-limitations)

## Security and Limitations
The app, as of now only, only protects the screen.
Passwords are stored in a private database on the device and are only sent to the configured server to change them.
This leaves the passwords unencrypted in storage.

For the average user, this shouldn't pose any risk since the data is only ever accessable by the app itself.
The private storage section used to house the database with the passwords can only be accessed when the phone's security is circumvented by rooting the device.
This is NEVER the case by default. If your device is rooted you will know since you either did it yourself or someone did it for you.
The process of rooting the device will reset all personal data and therefore protect the stored data. 
Further protection will be implemented if concerns arise regarding methods of extraction of data from storage on non-rooted devices.

If your device is rooted, you have to wait, whether I find the time to implement support for full encryption.
