#!/bin/bash -x

#for fullpath in ./KISS2/acm-sigda-mcnc/s*.kiss*
#for fullpath in ./KISS2/ism/*.kiss*
#for fullpath in ./KISS2/lgsynth91/*.kiss*

now=$(date +"%T")
type=XOR
printf "[FINAL] Result Current time : $now" >> result.txt


java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/apex2.bench  31  $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/apex4.bench  128 $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/c1355.bench  27  $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/c1908.bench  44  $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/c2670.bench  60  $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/c3540.bench  83  $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/c432.bench   8   $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/c499.bench   10  $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/c5315.bench  115 $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/c7552.bench  128 $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/c880.bench   19  $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/dalu.bench   115 $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/des.bench    128 $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/ex1010.bench 128 $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/ex5.bench    53  $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/i4.bench     17  $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/i7.bench     66  $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/i8.bench     123 $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/i9.bench     52  $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/k2.bench     91  $type | tee /dev/tty | grep -F "FINAL" >> result.txt
java Netlist_Handler encSecureNew ./Benchmark/SAT-benchmark/original/seq.bench    128 $type | tee /dev/tty | grep -F "FINAL" >> result.txt
