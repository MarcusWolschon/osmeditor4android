

This version of the library has been modified to for vespucci use to use BoundingBox instead of RectF and contains numerous fixes to support adding of points/nodes.

# Android R-Tree #

R-Tree built for usage in Android applications, originally adapted from an original by
Colonel Thirty Two. Currently uses RectFs for its rectangle storage, but could easily be
adapted to use standard Rects, or even generic...ified... to accept either or.

Right now you can store anything that implements the BoundedObject interface (which is
the only other file provided). It's extremely easy to adapt - you have to implement one
method - getBounds() - which returns a RectF that holds the rectangle your object covers.

## How to use it ##

Easy! Drop RTree.java and BoundedObject.java into your Android project wherever it's needed.
You can keep my package name (which would be pretty sweet) or you can adapt it to fit your
project structure. Once you're inside your project, initialize the object with

```java
RTree yourTree = new RTree();
```

And you're ready to go. Add objects with

```java
yourTree.insert(BoundedObject object);
```

To return the objects that are within a certain rectangle, pass it an ArrayList and it will
populate it with your results like this

```java
ArrayList<BoundedObject> results = new ArrayList<BoundedObject>();
yourTree.query(results, RectF area);
```

To return all objects in the tree, don't give it an area to work with

```java
ArrayList<BoundedObject> results = new ArrayList<BoundedObject>();
yourTree.query(results);
```

-------------------------------------------------------------------------------------------

Copyright 2011 Colonel Thirty Two. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are
permitted provided that the following conditions are met:

   1. Redistributions of source code must retain the above copyright notice, this list of
      conditions and the following disclaimer.

   2. Redistributions in binary form must reproduce the above copyright notice, this list
      of conditions and the following disclaimer in the documentation and/or other materials
      provided with the distribution.

THIS SOFTWARE IS PROVIDED BY Colonel Thirty Two ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Colonel Thirty Two OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those of the
authors and should not be interpreted as representing official policies, either expressed
or implied, of Colonel Thirty Two.
