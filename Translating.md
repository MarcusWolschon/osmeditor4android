All the translatable text in Vespucci has been concentrated into one file per language called 'strings.xml'.

There are strings elsewhere in Vespucci that would be nice to be translated, but it is currently technically difficult to make them available. The background layer descriptions are one example.

## transifex ##

The translations are now managed on transifex: https://www.transifex.com/projects/p/vespucci/

Currently transfex polls the strings.xml in the 0.9 branch for changes, once the branch is merged into trunk that should change to the trunk version. Further automation of pulling the translations off transifex would be nice.