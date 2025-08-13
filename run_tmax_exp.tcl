#set_message_info -stop_on -id M114
read_netlist ./test.v
run_build_model dut
set_drc ./netlist_exp.spf
run_drc
#add_faults final_out -stuck 0
#add_faults oracle -stuck 0
add_faults -all
#high abort limit ensures testability
set_atpg -abort_limit 1000000
run_atpg -auto
#report_faults -class ND
#report_faults -class UD
report_faults -class NC
report_faults -class NO
report_patterns -all
exit
