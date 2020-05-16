# Cafecompare [![Build Status](https://travis-ci.com/GraxCode/cafecompare.svg?branch=master)](https://travis-ci.com/GraxCode/dalvikgate) [![Release](https://img.shields.io/github/v/release/GraxCode/cafecompare)](https://github.com/GraxCode/cafecompare/releases) [![Downloads](https://img.shields.io/github/downloads/GraxCode/cafecompare/total)](https://github.com/GraxCode/cafecompare/releases)
Cafecompare is a GUI application for analysis and comparison of java archives and class files. 
Similar to github commits the file diffs are highlighted. Also works with obfuscated code. 
Cafecompare is the first tool ever to support remapping class names of an obfuscated jar archive by similarity to the other jar archive (with libraries or an old source file, where mappings can be taken from). This introduces a new era of deobfuscation.
![Screenshot 1](https://i.imgur.com/up1FTqp.png)

## Getting started
To compare two jar archives or two classes, you have to add them to the trees on the left.
On the top goes the old jar file, on the bottom the new version.
The left side of the decompiler panel will show the old code, the right side the new code.
Deleted code will be marked with red color in the left panel, inserted code will be green in the right panel.
If you want to remap class files by similarity, make sure the obfuscated file is placed in the bottom tree, and the (unobfuscated) source file in the top one. Be aware that this could take VERY long!
Have fun comparing and deobfuscating!

## Donate
If I saved your time (or your project) and you want to buy me a coffee you can do so here: [![Donate with Bitcoin](https://en.cryptobadges.io/badge/micro/37f6MxNoyyksgh3hWtbh9UKkkGDSAoHCtT)](https://en.cryptobadges.io/donate/37f6MxNoyyksgh3hWtbh9UKkkGDSAoHCtT)

## License
Cafecompare is licensed under the GNU General Public License 3.0
