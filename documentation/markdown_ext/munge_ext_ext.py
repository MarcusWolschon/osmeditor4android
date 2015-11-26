"""
# 

Rewrite links to markdown files to html based on code nicked from mkdocs
2015 Simon Poole

The original code was licensed on the following terms:

Copyright 2014, Tom Christie. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

"""
from __future__ import unicode_literals
import logging

from markdown.extensions import Extension
from markdown.treeprocessors import Treeprocessor

from mkdocs import utils
from mkdocs.exceptions import MarkdownNotFound

log = logging.getLogger(__name__)


def _iter(node):
    # TODO: Remove when dropping Python 2.6. Replace this
    # function call with note.iter()
    return [node] + node.findall('.//*')


def munge(url):

    scheme, netloc, path, params, query, fragment = (
        utils.urlparse(url))

    if scheme or netloc or not path:
        # Ignore URLs unless they are a relative link to a markdown file.
        return url

    if not path.endswith(".md"):
        return url
    else:
        path = path.replace(".md",".html");

    # Convert the .md hyperlink to a relative hyperlink to the HTML page.
    fragments = (scheme, netloc, path, params, query, fragment)
    url = utils.urlunparse(fragments)
    return url


class MungeExtensionTreeprocessor(Treeprocessor):

    def run(self, root):
        """
        Change the extension of linked .md docs to .html

        Iterates through the full document tree looking for specific
        tags and then makes them relative based on the site navigation
        """

        for element in _iter(root):

            if element.tag == 'a':
                key = 'href'
            elif element.tag == 'img':
                key = 'src'
            else:
                continue

            url = element.get(key)
            new_url = munge(url)
            element.set(key, new_url)

        return root


class MungeExtensionExtension(Extension):
    """
    The Extension class is what we pass to markdown, it then
    registers the Treeprocessor.
    """

    def extendMarkdown(self, md, md_globals):
        mungeextension = MungeExtensionTreeprocessor()
        md.treeprocessors.add("mungeextension", mungeextension, "_end")

def makeExtension(*args, **kwargs):
    return MungeExtensionExtension(*args, **kwargs)
