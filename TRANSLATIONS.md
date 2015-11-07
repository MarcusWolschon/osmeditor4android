All the translatable text in Vespucci has been concentrated into one file per language called 'strings.xml'.

There are strings elsewhere in Vespucci that would be nice to be translated, but it is currently technically difficult to make them available. The background layer descriptions are one example.

## transifex

The translations are managed on transifex: https://www.transifex.com/projects/p/vespucci/

NOTE: for building we assume for now that the SVN repository contains the current language files, if you are adding or changing strings it is your responsibility to retrieve current translations and check them in.

Currently transfex polls the strings.xml in the 0.9 branch for changes, once the branch is merged into trunk that should change to the trunk version. 

### Setting up transifex locally and retrieving translations

#### Install

get the transifex tx tool see http://support.transifex.com/customer/portal/articles/995605-installation

the repository already includes a suitable .tx directory with config file, if that does not work for you do the following

	in your OSMEditor directory execute

	tx init

	tx set --auto-local -r vespucci.main "res\values-<lang>\strings.xml" --source-lang en --source-file res\values\strings.xml --execute
	(my installation is on a windows machine on *IX "\" -> "/")

	edit .tx/config and add
	
	lang_map = cs: cs-rCZ 
	
	to the [vespucci.main] section
 
 #### Retrieving current translations
 
    tx pull -a
 
 will retrieve all translations configured above, skipping up to date translation files and creating new directories and strings.xml files for new languages.
 
