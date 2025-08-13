####BUILD

read_netlist ./temp_flt.v
read_netlist ./Lib/stdcells.v
run_build_model dut

####DRC
set_drc ./temp.spf
add_pi_constraints 1 locking_key_5
add_pi_constraints 1 locking_key_26

run_drc
add_faults -all

run_atpg
report_faults -class UD AU
#report_faults -all
#report_faults -class ND
exit


