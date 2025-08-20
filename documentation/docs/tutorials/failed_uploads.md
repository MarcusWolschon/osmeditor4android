# Dealing with failed uploads

_Simon Poole_

[This is a slightly updated blog post from spring 2025, while Vespucci handles this as far as possible automatically now, it is a good idea to understand what causes the issues.]

Some of the readers may have experienced the sinking feeling when they just attempted to upload dozens if not 100s of changes and a network glitch resulted in the editor app failing the upload.

What's the problem when that happens?

It is literally that your editing app doesn't know if any new objects it created locally have been successfully created on the server or not. If they have been created, retrying the upload will create duplicates, and if you don't retry you potentially loose your work.

This post is related to work that I'm did for version 21 of Vespucci that automates all of this, but the interesting thing is that you can easily handle many of the cases manually without editor support.

As you may know OSM changesets are not atomic, but individual uploads are (yes, you can upload multiple times in to the same changeset). With other words if your upload succeeded, but your editing app lost the response from the server, all your changes will have been successfully made, and, the other way around, if your upload fails it will fail completely.

The other point to note is that changesets are opened in a separate operation and are only automatically closed after a timeout of an hour if they are not explicitly closed after the upload by your editing app.

A normal upload operation will roughly contain the following steps

* open the changeset.
* upload the data  and process the response. Depending on your editing app it will update the downloaded data in place, or throw it away and re-download from the server.
* close the changeset.

This means that there are simple cases that you can handle manually, assuming you are not uploading more than once per changeset (more on that later):

* opening the changeset fails: if you go to your account and check your edits, you should see either no new changeset or an open one with no changes. _You can safely retry the upload_.
* something went wrong uploading the data: if you check your edits, you should see an open changeset that either 
    1. is empty, that is the upload failed completely.  _You can safely retry the upload_.
    2. contains all your uploaded edits. In this case you should discard your edits and re-download data from the server.
* closing the changeset failed, this is normally harmless and your editing app should probably not even report this, as all your changes have been saved and your editor has current state. In your OSM account you will simply see that the changeset hasn't been closed till an hour later.

### Now for the the tricky bits### 

You would assume that these issues could be avoided completely by not uploading multiple times per changeset. However the popular, at least in JOSM and [Vespucci][1] _Upload selection_ functionality and chunked uploads in to separate changesets have exactly the same issue: you have pending changes in your editors data that you haven't attempted to upload yet, and that possibly, to make matters worse, refer to elements that you did try to upload and that failed.

If the upload failed completely as in 1. above, you are good to go and can retry, but scenario 2. doesn't help you. Why is that? 

When an editor creates an new object it assigns a negative value id to it, then when you upload the data the OSM API replaces it with the actual one that will be used in the database and reports that back in the response to the editor. If you don't receive the response you don't have the mapping between placeholder id and actual id.

Assume you've created two completely new ways, with one common node. You will have actually created 5 new nodes, n-1 to n-5 and two ways w-1 and w-2.

~~~
n-1
n-2
n-3
n-4
n-5
w-1 [n-1, n-2, n-3]
w-2 [n-4, n-2, n-3]
~~~

Now you've uploaded the 5 nodes as first changeset and that fails. So even if your 5 nodes were uploaded completely you have no way to change the references in w-1 and w-2 to match the ids of the nodes that were successfully uploaded. This is the JOSM "importing a gazillion objects" scenario in which you are uploading dozen of changesets with 20'000 nodes before getting to the ways and it fails halfway through. leaving a gazillion orphan nodes behind.

You definitely can't reasonably fix this manually. Note "reasonably": you can naturally save your OSM data to a file and then manually change the ids, this even works for toy data as in the example, but is hopelessly tedious for any larger edit.

This is what I'm working on for Vespucci 21, besides providing a simple way of retrying an upload when it is safe, I'm adding functionality to patch data in the app in such scenarios. In principle this is quite simple, we can download all the changes in a changeset from the API with https://wiki.openstreetmap.org/wiki/API_v0.6#Download:_GET_/api/0.6/changeset/#id/download then update the data from it. 

This is straightforward for updates and deletions of existing objects as we can identify them by id and only need to increase the version by 1. Newly created objects however need to be identified by using heuristics as the API provided osmChanges format data doesn't (can't?) provide the placeholder id to actual id mapping.

The heuristics will fail (as in use the wrong id mapping) for untagged elements with exactly the same geometry (consider untagged nodes on top of each other), but that is probably survivable in the grand scheme of things, 
 
See https://github.com/openstreetmap/openstreetmap-website/issues/2201 for some discussions on potential ways to avoid these issues in the API. 

[1] Vespucci 21.2 will improve the behaviour for selective uploads by enforcing that parent elements of the selection are added to the upload. See https://github.com/MarcusWolschon/osmeditor4android/pull/2953 This doesn't just address the failed upload issue, it ensures that the data on the server has the expected state (see the referenced issue in the PR).]  