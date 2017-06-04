# Reporting Vespucci issues
_by Simon Poole_

It is a truism that software has bugs and while as a developer it would be nice to say that they are all somebody else's fault, we are probably are all just as bad as each other with respect to slip-ups. In the case of Vespucci the additional complication is that we are dealing with at least 26 different Android versions and 100s of different devices, each with its own manufacturer tweaks to hard- and software. 

So unluckily now and then you might experience a crash. I do have to say outside of provoked crashes and early dev builds I haven't had one for ages, but then I use a relative mainstream device for mapping and don't try to edit gigantic areas. The important thing is to either get the issue fixed, or at least find out how to avoid it in the future. 

Those readers that have had the unpleasant experience know that post-crash you will be offered the chance to submit a crash report. We do this with [ACRA](http://www.acra.ch/) and store the reports on a private Acralyser server. ACRA gives us a lot of information on the HW and SW configuration of the device, a short excerpt of the system log file and a stack trace indicating what the immediate cause of the crash was and where in the Vespucci or Android code it happened. It does not store any personal information of the user in question, in principle we could ask for an e-mail address, but I would prefer not to for data protection reasons.  Note on the side: there are some situations in which we'll produce a stack trace just to document an unusual situation, if you don't get an additional warning in general there is no reason to be concerned.

Now if you experienced a real crash (real: as in Vespucci restarted or stopped completely) please submit the crash report, but further please check our [issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues) and our [twitter account](https://twitter.com/vespucci_editor). If your problem doesn't seem to be known or not already fixed, please open a new issue pointing out that you submitted a report, this is the only way we can get in direct contact with you. Complaining on Google Play or continuously sending the same crash (don't forget it might be related to your device or your editing habits) won't help.

Happy mapping! 
