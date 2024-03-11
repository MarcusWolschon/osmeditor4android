# Conflict resolution

Vespucci provides basic facility to resolve version conflicts on upload. In general is it best to avoid them happening, particularly in areas with lots of editing activity by downloading only small areas, refreshing them often and saving changes frequently.

The OSM API reports conflicts one by one. The upload will abort at the first error and you will need to resolve that and then re-upload, fix the next issue and so on, exceptions noted below. If you suspect that your edits have caused more than a handful of conflicts you can save the edits either by saving to a JOSM compatible OSM file or by exporting the changes to a OSC file and then try to resolve the conflicts in JOSM, if you prefer using a desktop computer.

### Version conflicts

The most common conflict is when an object has been modified or deleted while you were editing and the version number of the object that you modified is lower than the current one on the OpenStreetMap servers. In such a case you will be presented with the options to cancel, or resolve the issue by 

* _using the local version_ - this simply increments the version of the local element. This leads to your local version overwriting any changes made on the server to the element.
* _merging tags in to the server version_ - this retains the server side geometry but merges the local tags in to the server version of the tags, you will be notified if there are conflict between the tags and you can address any tagging issues before re-trying the upload.
* _merging tags in to the local version_ - the behaviour is as above, but this variant retains the local geometry.
* _using the server version_ - this replaces the local version of the element with the server version, overwriting any changes you made locally.


### Referential conflicts

While Vespucci attempts to avoid this happening, a referential conflict can occur when you delete an object locally, and the server still has objects that refer to the deleted one, which haven't been modified accordingly or you have local elements that refer to elements that have been deleted on the server.

In the first scenario you can resolve the issue by (for each element)

* _undoing the local deletion_ - this will utilize Vespuccis undo system to locate and undo the undo checkpoint that deleted the element, note that this may result in other elements being changed too.
* _deleting the references on the server_ this will download the relevant elements and delete them, adding them to the upload for deletion on the server.

In the second scenario, you can, again per element

* _delete the element locally_ - this will delete the element locally.
* _undelete the element on the server_ - this will upload the local element as a new, no longer deleted, version.

### Deleting an element that has already been deleted

This is the easiest to handle conflict and Vespucci will automatically retry the upload after removing the deleted element from the to be uploaded data.
