####BUILD

read_netlist ./temp1_flt.v
read_netlist ./Lib/stdcells.v
run_build_model dut

####DRC
set_drc ./temp1.spf
####REPLACE
run_drc
add_faults -all

run_atpg
report_faults -class UD AU
#report_faults -all
#report_faults -class ND
exit


