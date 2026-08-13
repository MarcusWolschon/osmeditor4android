## Setting up image storage locations

Vespucci V21.2 has added support for uploading individual images both to Panoramax instances and Wikimedia Commons. Currently you can upload images from 
the element selection modes, automatically adding a corresponding link to the OSM element and from the image viewer.

Before you upload you need to configure and authorize the destinations:

### Adding and authorizing a Panoramax instance

Note: we currently only support the Panoramax native authentication process and not any instance specific methods. 

You will need to create an account on your target instance first, 
then navigate to [Advanced preferences - Camera and images settings - Image storage](/help/en/Advanced%20preferences/#image-storage). 

<img src="../images/panoramax_setup_1.png" width="250"/>

If you are using a public instance that has
been registered with the Panoramax directory, you can simply click the _+_ button and then select _Add from Panoramax instances list_ and then save the configuration by pressing
_OK_ in the modal. If you are using a private instance you will need to manually create an entry with the API URL and a name.
 
 <img src="../images/panoramax_setup_2.png" width="250"/>
 
 Now you need to authorize access from Vespucci to your Panoramax account. Select _Authorize_ and login to your account and authorize access.
 
 <img src="../images/panoramax_setup_3.png" width="250"/>
 
 You are done.
 
### Authorize access to Wikimedia Commons
 
 You will need to create an account for access to Wikimedia and Wikipedia first.
 
 Navigate to [Advanced preferences - Camera and images settings - Image storage](/help/en/Advanced%20preferences/#image-storage). There should already be an entry 
 for Wikimedia Commons, if it is missing you can manually add the entry:
 
 - click the _+_ button, select _Add manually..._
 - add an entry for _Wikimedia Commons_, with type _WIKIMEDIA_COMMONS_ and URL _https://commons.wikimedia.org_
 - save by clicking _OK_
 
 Now you need to authorize access from Vespucci, we will do this by creating a personal key for your account. This process is a bit involved, but is still easy to do
 even on a mobile device.
 
Select _Authorize_ and login to your account. 

 <img src="../images/wikimedia_commons_1.png" width="250"/>
 
You should see 
 
<img src="../images/wikimedia_commons_2.png" width="250"/>
 
Scroll down to _New OAUth consumer application_
 
<img src="../images/wikimedia_commons_3.png" width="250"/>
 
You only need to set, change or verify the settings mentioned here, everything else should already be correct.
 
Set the _Application name_ to an unique value, note that Wikimedia doesn't support deleting unused key entries, so if you are redoing 
this you will need to use a different name each time.
 
Add an application description, check the _This consumer is for use only by_ box, check that _Client is confidential_ is checked and _Request authorization for specific permissions._ is selected.
 
<img src="../images/wikimedia_commons_4.png" width="250"/>
 
Scroll down to _Applicable grants:_
 
Select 
 
- _High-volume (bot) access_
- _Edit existing pages_
- _Create, edit, and move pages_
- _Upload new files_
 
<img src="../images/wikimedia_commons_5.png" width="250"/>
 
Scroll down to the final checkbox, except the terms and click _Propose consumer_.

<img src="../images/wikimedia_commons_6.png" width="250"/>
 
If all goes well, you will now see a page with the consumer key, the key will be automatically extracted and added to the configuration, then the page closed.
 
Note: by its very nature the key extraction process is quite brittle, that is, it depends on the specifics of the web page Wikimedia Commons generates. If the
mechanism stops working, please report it on our gihub repo.
 