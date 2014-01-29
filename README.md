GlassWebNotes
=============
By Victor Kaiser-Pendergrast

## About##
An application demonstrating how to connect the Glass GDK with [App Engine](https://developers.google.com/appengine/docs/whatisgoogleappengine) [Endpoints](https://developers.google.com/appengine/docs/java/endpoints/). Sign-in is handled through OAuth for Devices. See [here](https://developers.google.com/accounts/docs/OAuth2ForDevices) for Google's documentation on it.

Notes are stored on the server component, which is a Java [Google App Engine](https://developers.google.com/appengine/docs/whatisgoogleappengine) project. The server makes use of the [Endpoints API](https://developers.google.com/appengine/docs/java/endpoints/)

##Glass Application##
The code for the Glass application is located in the [GlassNotesApp](https://github.com/victorkp/GlassWebNotes/tree/master/GlassNotesApp) folder. It is a fairly straightforward Glass app.

If anything, the parts to note are located under the [auth](https://github.com/victorkp/GlassWebNotes/tree/master/GlassNotesApp/src/com/victor/kaiser/pendergrast/glass/notes/auth) subpackage. These classes are used to handle OAuth authentication.

Also, see the classes under the [api](https://github.com/victorkp/GlassWebNotes/tree/master/GlassNotesApp/src/com/victor/kaiser/pendergrast/glass/notes/api) subpackage, which demonstrate making API calls to the server through HTTP GETs and POSTs.

##App Engine Component##
The App Engine side is also fairly basic; you can find it in the [GlassNotesServer](https://github.com/victorkp/GlassWebNotes/tree/master/GlassNotesServer) folder. The [Endpoints API](https://developers.google.com/appengine/docs/java/endpoints/) makes OAuth 2.0 rather easy to implement, see the code in [EndpointApi](https://github.com/victorkp/GlassWebNotes/blob/master/GlassNotesServer/src/com/victor/kaiser/pendergrast/glass/server/EndpointAPI.java) for how easy it is to use OAuth 2.0 on App Engine.

User notes are saved with the [Datastore API](https://developers.google.com/appengine/docs/java/datastore/) and a write cache. A [cron](https://github.com/victorkp/GlassWebNotes/blob/master/GlassNotesServer/war/WEB-INF/cron.xml) job is used to periodically persist all the data in the write cache to the [datastore](https://developers.google.com/appengine/docs/java/datastore/). See [UserDatabase](https://github.com/victorkp/GlassWebNotes/blob/master/GlassNotesServer/src/com/victor/kaiser/pendergrast/glass/server/data/UserDatabase.java) for how the write cache is implemented.