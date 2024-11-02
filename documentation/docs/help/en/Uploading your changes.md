# Uploading your changes
   
While Vespucci allows you to save your edits to local files on your device, most use cases center around uploading and publishing your data 
to a server providing the [OpenStreetMap editing API](https://wiki.openstreetmap.org/wiki/API_v0.6) and that will be the instance available on [openstreetmap.org](https://openstreetmap.org) if you are contributing to regular OpenStreetMap. The following information only concerns itself with this standard configuration, other APIs and non-standard authorizations methods can be configured in the [data layer configuration](Main%20map%20display.md#layer_control). 

## Authorization

To successfully publish your edits you need to authorize your Vespucci on your device to access your account on openstreetmap.prg on your behalf. You can start the authorization process by either selecting the corresponding option in the configuration dialog display on initial install of the app, by starting an upload process or by using the _Authorize OAuth_ option from the [Tools menu](Main%20map%20display.md#tools).

_Note_ that you will need to have your OSM display name and password available and be connected to the Internet to successfully complete authorization, if you don't already have an account you will have to create one outside of Vespucci prior to starting authorization. While it technically would be possible to create the account from inside the app, doing so invokes additional requirements from google that we cannot fulfill for legal reasons.

During the authorization process you will be connected to openstreetmap.org and asked to login to your account, you then need to confirm the authorization and will be returned back to Vespuccis map map display.

If you have pending edits and for whatever reason cannot authorize, Vespucci reliably saves your edits on your device until you upload them or explicitly delete them. If you want to be extra safe you can [save them to a local file](Main%20map%20display.md#file) in OCS or (J)OSM format that you can read with Vespucci or import in to JOSM. 

## Uploading

To upload your edits you can either select _Upload data to OSM server_ from the [Transfer menu](Main%20map%20display.md#trasfer), or select _Upload element_ from the overflow menu in any of the element selection modes or multi-select. The later will only upload the selected elements.

The _Upload changes_ dialog display two tabs _Changes_ and _Properties_. The _Changes_ tab displays a list of all the OSM elements that will be affected by the upload, the type of change and an information button that allows you to view further information on the element and jump to it to make corrections. The _Properties_ tab allows you to set a _comment_
for this upload and document that _source_ that was used to create the changes (for example _survey_ if you were personally present at the location in question). Prior entries in both
fields are retained and can be used when appropriate.

It is considered best practice to provide meaningful information for other mappers in these fields. However Vespucci will automatically include a summary of your changes (starting with version 20.2) and a list of all imagery layers used for the current set of changes. If you leave the comment field empty it will include text pointing to the automatically generated summary.

## Upload options

- _Close changeset_ close the changeset after the upload. If this is unchecked, changesets will be left open and further uploads will be appended. Note that the changeset may be automatically closed by the API, this is detected on upload and a new changeset will be created. The default setting for this is enabled.
- _Close open changeset_ only displayed if an open changeset has been detected (that is _Close changeset_ must be disabled). Close the current changeset after upload. This setting only applies to the current upload. The default setting for this is disabled.
- _Request review_ add a tag asking for a review of the changes. Note this does not stop your changes from being immediately available after you have completed the upload, it is 
simply an indication to other contributors and they might or might not actually review your edits. The default setting for this is disabled.
- _Warn if comment is empty_ As described above it is best practice to add a meaningful comment to your upload, if you don't want to do this, unchecking this option
will disable warnings that the comment is empty and disable the automatic switch to the _Properties_ tab prior to the upload. This is available from version 20.2 on, the default setting for this is enabled.