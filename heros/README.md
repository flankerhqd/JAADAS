![Logo](logo/heros-logo.png)
Heros IFDS/IDE Solver
=====================
Heros is a generic implementation of an IFDS/IDE Solver that can be plugged into existing, Java-based program analysis frameworks. A reference connector exists for the [Soot][1] framework.

Heros...
* supports solving both IFDS and IDE problems,
* is multi-threaded and thus highly scalable,
* provides a simple programming interface, and
* is fully generic, i.e., can be used to formulate program analysis problems for different programming languages.

Who are the developers of Heros?
--------------------------------
Heros was developed and is maintained by [Eric Bodden][2].

Why is Heros called Heros?
--------------------------
The name contains (in a different order) the first characters of the last names Reps, Horwitz and Sagiv, the original inventors of the IFDS/IDE frameworks. Heros is pronounced like the Greek word, not like heroes in English.

What is IFDS/IDE in the first place?
------------------------------------
[IFDS][3] is a general framework for solving inter-procedural, finite, distributive subset problems in a flow-sensitive, fully context-sensitive manner. From a user's perspective, IFDS allows static program analysis in a template-driven manner. Users simply define flow functions for an analysis problem but don't need to worry about solving the analysis problem. The latter is automatically taken care of by the solver, in this case by Heros.
[IDE][4] is an extension of IFDS that allows more expressive computations. Heros implements an IDE solver and supports IFDS problems as special cases of IDE.

What are the unique features of Heros over other IFDS/IDE solvers?
------------------------------------------------------------------
To the best of our knowledge there exist at least two other similar solvers implemented in Java. [Wala][5] implements a solver that supports IFDS but not IDE. The solver is highly scalable but in our eyes requires more verbose definitions of client analyses. Heros is fully multi-threaded, while Wala's solver is not. There also exists a Scala-based solver by [Nomair A. Naeem, Ondrej Lhotak, and Jonathan Rodriguez][6]. This implementation does support IDE, and there exists a multi-threaded version of it, but as of yet the implementation is not publicly available.

Why did you create Heros?
-------------------------
One reason is that we found that no existing IFDS/IDE solver satisfied all our needs. The solver in Wala was available but only supported IFDs. Moreover we desired a more simple client interface. The solver by Naeem, Lhotak and Rodriguez was written in Scala. Further we wanted a solver that could be used with multiple programming languages.

The second, and probably better reason is that we found that IFDS/IDE is really useful and that probably there should be a solver that the community can build on, extend and improve over the years. But this requires clean code and documentation. When designing Heros we took special care to provide just that.

What is this all about support for multiple programming languages?
------------------------------------------------------------------
Solving an IFDS/IDE analysis problem basically requires three things:

1. An IFDS/IDE solver.
2. An implementation of an inter-procedural control-flow graph (ICFG).
3. The definition of an IFDS/IDE analysis problem in the form of flow functions.

The solver in heros is fully generic. It can be combined with any form of ICFG. Through Java's generic type variables, Heros abstracts from any concrete types such as statements and methods. To connect Heros to a program-analysis framework for a particular language, all one needs to do is to implement a special version of the ICFG. We provide a (reference implementation)[8] for Soot. Also the IFDS/IDE analysis problems need to be defined with respect to the actual programming language's constructs and semantics. They are not generic. The entire solver, however, can be reused as is. We are currently working on connecting Heros to a C/C++ compiler.

How can I contribute to Heros?
------------------------------
Contributions are more than welcome! It is easiest to fork the project on Github. Then make your modifications on the fork and send us a pull request. This will allow us easy re-integration.

Under what License can I use Heros?
-----------------------------------
Heros is released under LGPL - see [LICENSE.txt][7] for details.

How can I obtain Heros?
------------------------

Pre-compiled versions of Heros are available on our [nightly build server](http://ssebuild.cased.de/nightly/heros/). "heros-trunk.jar" is a complete build containing sallall required libraries. "herosclasses-trunk.jar" only contains the Heros classes and lets you provide the required libraries on your own.

[1]: http://www.sable.mcgill.ca/soot/
[2]: http://bodden.de/
[3]: http://dx.doi.org/10.1145/199448.199462
[4]: http://dx.doi.org/10.1016/0304-3975(96)00072-2
[5]: http://wala.sf.net/
[6]: http://dx.doi.org/10.1007/978-3-642-11970-5_8
[7]: https://github.com/Sable/heros/blob/master/LICENSE.txt
[8]: https://github.com/Sable/soot/blob/master/src/soot/jimple/toolkits/ide/icfg/JimpleBasedInterproceduralCFG.java
