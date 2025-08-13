####BUILD
read_netlist ./dut.v
read_netlist ./miter.v
run_build_model miter


####DRC
set_drc ./netlist.spf
#add_pi_constraints 0 keyinput0A
#add_pi_constraints 0 keyinput0B
#add_pi_constraints 0 keyinput1A
#add_pi_constraints 0 keyinput1B
report_pi_constraints
run_drc

####TEST
add_faults final_out -stuck 0
add_faults oracle -stuck 0
#add_faults -all
#high abort limit ensures testability
set smallBound 50000000
set largeBound 500000000
set_atpg -abort_limit $largeBound
#-decision random
#-analyze_untestable_faults
set_learning -implication high




#report_settings -all
run_atpg
#-ndetects 1
report_faults -class DT
report_faults -class UD
report_faults -class ND
report_patterns -all
exit
