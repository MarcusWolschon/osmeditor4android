# Vespucci End-User Documentation

The system tries to maximize the use of the same texts both for on device help as well as for the vespucci.io website. 

The files are found in the documentation directory, layout:

    documentation			top level documentation directory
    	/docs			contains markdown source files and images
    		/help
    			/de			language specific files for on device help and website
    			/en
    			/es 
    			/fr
    			/images		images, mainly icons for the above
    			.....
    		/tutorials		texts and images not included on device for size reasons
    			/images
    			....
    			.....
    		CNAME			domain this appears under for github pages
    		index.md		top level page for vespucci.io
    	/flatly-custom		slightly customized mkdocs theme 
    	mkdocs.yml			mkdocs

## vespucci.io website

The static website is produced with mkdocs, see mkdocs.org .

You can serve a local version on your machine by running
    mkdocs serve
and push the site to our github pages with
    mkdocs gh-deploy --clean
Note: any manual changes to the github pages will be lost! 
	

## On device help

The source for the help files is in the language specific directories, currently these need to be converted from markdown to html manually pre-build and then copied to the corresponding directories in assets. While this is a bit of a pain, it is more faster than doing it on the fly and eliminate the need to include a 3rd part markdown support library (which all have issues of one or an another kind).

The help files need to be named the same as in the resource file "helptopics.xml". The names of the files can be translated, but the actual help files then need to be renamed too, you can however leave the default versions in if you have not translated all files. 

Help with automating the build process would be welcome.
