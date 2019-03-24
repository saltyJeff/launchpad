# Launchpad
Do I have time to update documentation?

No?

## Developer notes
Everything is a warning:

Java doesn't support unsigned stuff, and apparently
our codebase loves them some unsigned, so anything with a to U type such as
to UByte()/ULong... will create warning

Also don't use the XBee serial library on Gradle, download the jars
from their github.