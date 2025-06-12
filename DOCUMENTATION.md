# Vespucci End-User Documentation

The system tries to maximize the use of the same texts both for on device help as well as for the vespucci.io website. 

The files are found in the documentation directory, layout:

    documentation			top level documentation directory
    	/docs			contains markdown source files and images
    		/help                   language specific files for on device help and website
    			/de			
    			/en
    			/es 
    			/fr
    			/images		images, mainly icons for the above
    			.....
    	   /help-rtl                rtl scripts specific files for on device help and website
    	      /ar
    	      .....  
    		/tutorials		texts and images not included on device for size reasons
    			/images
    			....
    			.....
    	    /playstore              text for the playstore listing
    		CNAME			domain this appears under for github pages
    		index.md		top level page for vespucci.io
        html_template.tpl           template for on device html
    	html_template_rtl.tpl       rtl template for on device html
    	mkdocs.yml			mkdocs configuration file

## vespucci.io website

The static website is produced with [mkdocs](http://mkdocs.org) (requires version 1.6.x).

You can serve a local version on your machine by running (you will need to install the [material theme](https://squidfunk.github.io/mkdocs-material/)):

    mkdocs serve

You can push the site to our GitHub Pages with:

    mkdocs gh-deploy --clean

Note: any manual changes to the GitHub Pages will be lost! 
	

## On-device help

The source for the help files is in the language specific directories, these need to be converted to html format and copied to the Android assets directory for on device use by running the `markdownToHtml` task. This is only necessary if you have changed something, to keep the build process simple the current generated html files are stored in our source code repository too. 

The help files need to be named the same as in the resource file "helptopics.xml". The names of the files can be translated, but the actual help files then need to be renamed too (note in practice this currently doesn't work due to limitations of Android file names), you can however leave the default versions in if you have not translated all files. 

## Generating HTML for inclusion in the APK

Run gradle task

    markdownToHtml
    
## Text for playstore listing

The title, description and long text of the Vespucci playstore listing can be found in the playstore directory. The text is translated on transifex and updating translations via the gradle task will pull the translated texts in to build/tmp/playstore/...
