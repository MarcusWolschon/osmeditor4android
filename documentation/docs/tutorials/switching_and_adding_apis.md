## Switching and adding OpenStreetMap data APIs
_Simon Poole 20250826_

Vespucci supports utilizing multiple instances of the OpenStreetMap API, and has  configurations for the default OpenStreetMap instance, the developer sandbox, and OpenHistoricalMap (since V21.1.2). Currently only one configuration can be active at any one time.

### Switching between existing configurations

##### Open the layer modal

<img src="../images/apis_layer_modal.png" width="250"/>

##### Open the overflow menu for the OpenStreetMap data entry and click _Configure..._

<img src="../images/apis_configure.png" width="250"/>

##### Activate the target entry by checking the corresponding checkbox

<img src="../images/apis_select_target1.png" width="250"/>

<img src="../images/apis_select_target2.png" width="250"/>

##### Return to the main screen

#### Authorize the app with the new API

If you haven't used the newly selected API earlier and the API instances uses OAuth (all pre-configured API instances do) you will need to authorize the app. You will automatically be asked to do this on an upload if you didn't do this previously, however it is likely more convenient for you to start the process manually.

##### Open the _Tools_ menu

Depending on the dimensions of your device the tools menu will either be directly availably from the bottom menu (spanner button), or from the overflow (three dots) menu. 

<img src="../images/apis_tools_menu.png" width="250"/>

##### Select _Authorize OAuth..._

A browser-like window will open and you will be requested to login and confirm the authorization of the app.

<img src="../images/apis_webview.png" width="250"/>

### Adding a new configuration

Adding a new configuration is complicated by the fact that most target APIs require OAuth authorization and you need to provide a client key to support that. However if you simply want to modify an existing entry, for example by adding a read only source, you can _copy_ the entry and modify it retaining the exiting client keys, as long as the API URL remains the same.

<img src="../images/apis_copy.png" width="250"/>

#### Adding additional OAuth client keys

Obtain a new client key for the app, create a vespucci key file and add it to the configuration by using _Load keys from file..._.

An example key file can be found here (to add a key you only need the relevant entry): (keys2-default.txt)[https://github.com/MarcusWolschon/osmeditor4android/blob/master/src/main/assets/keys2-default.txt]



