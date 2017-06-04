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
    	/markdown_ext		trivial python markdown extension to munge file extensions 
    	mkdocs.yml			mkdocs

## vespucci.io website

The static website is produced with [mkdocs](http://mkdocs.org).

You can serve a local version on your machine by running:

    mkdocs serve

You can push the site to our GitHub Pages with:

    mkdocs gh-deploy --clean

Note: any manual changes to the GitHub Pages will be lost! 
	

## On-device help

The source for the help files is in the language specific directories, currently these need to be converted from Markdown to HTML manually pre-build and then copied to the corresponding directories in assets. This is best done with [Python-Markdown](https://pythonhosted.org/Markdown/). The directory markdown_ext contains an extension that will change the .md extension in Markdown links to .html for the processed files. While this is a bit of a pain, it is faster than doing it on the fly on the device and eliminates the need to include a third party Markdown support library (which all have issues of one kind or another).

The help files need to be named the same as in the resource file "helptopics.xml". The names of the files can be translated, but the actual help files then need to be renamed too (note in practice this currently doesn't work due to limitations of Android file names), you can however leave the default versions in if you have not translated all files. 

Help with automating the build process would be welcome.

## Generating HTML for inclusion in the APK

Run gradle task

    markdownToHtml
