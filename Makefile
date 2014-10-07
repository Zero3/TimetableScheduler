default: clean compile

compile:
	cd src; find . -name '*.java' > javafiles.lst
	mkdir -p classes
	cd src; javac -source 1.7 -classpath "../lib/*" -d ../classes @javafiles.lst
	mkdir -p bin
	jar cfm bin/TimetableScheduler.jar data/manifest.mf -C classes .
	mkdir -p bin/lib
	cp lib/* bin/lib/

clean:
	rm -f src/javafiles.lst
	rm -rf classes
	rm -rf bin