todo
====

* Call onComplete() on the player events Observable when the player is closed.
* On-device testing.
* Moving the playhead works inefficiently in the open access player: The target spine element is
  always reloaded from disk. If the requested spine element is the current spine element, the player
  should just simply seek rather than reloading.
