#!/bin/bash -x

#for fullpath in ./KISS2/acm-sigda-mcnc/s*.kiss*
#for fullpath in ./KISS2/ism/*.kiss*
#for fullpath in ./KISS2/lgsynth91/*.kiss*

now=$(date +"%T")
printf "\n\nCurrent time : $now" >> result.txt

for fullpath in ./Benchmark/SAT-benchmark/dac12/*_enc05.bench ./Benchmark/SAT-benchmark/dac12/*_enc10.bench

    #./Benchmark/SAT-benchmark/rnd/*_enc05.bench ./Benchmark/SAT-benchmark/rnd/*_enc10.bench

    #./Benchmark/SAT-benchmark/toc13mux/*_enc05.bench ./Benchmark/SAT-benchmark/toc13mux/*_enc10.bench
		#./Benchmark/SAT-benchmark/dac12/*_enc10.bench
		#./Benchmark/SAT-benchmark/rnd/*_enc10.bench ./Benchmark/SAT-benchmark/dac12/*_enc05.bench ./Benchmark/SAT-benchmark/dac12/*_enc10.bench
#for fullpath in ./KISS2/custom2/*.kiss*
do
    printf "\n"  >> result.txt
    java Netlist_Handler attackSingleRepeat $fullpath | tee /dev/tty | grep -F "FINAL" >> result.txt

done

