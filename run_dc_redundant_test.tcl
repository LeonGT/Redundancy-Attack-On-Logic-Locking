#######################################
# DC Compiler Options
#######################################
set_host_options -max_cores 16
define_design_lib WORK -path ./WORK

#######################################
# Libraries
#######################################
#set search_path "/usr/synopsys/syn/Q-2019.12-SP1/libraries/syn ./Lib"
set search_path "./Lib"
set symbol_library "generic.sdb"
set target_library "stdcells.db"
set link_library "stdcells.db"
set synthetic_library "stdcells.db"


read_verilog ./temp1.v
current_design dut

# check internal DC representation for design consistency
check_design

# verifies timing setup is complete
check_timing

# enable DC ultra optimizations 
compile -map_effort low
#compile_ultra

# reports
#report_area -hierarchy > reports/area.rpt
#report_power -hierarchy > reports/power.rpt
#report_timing -max_paths 30 > reports/timing.rpt

###########DFT
#set test_default_scan_style multiplexed_flip_flop
create_test_protocol -infer_asynch -infer_clock
#dft_drc

write_test_protocol -output temp1.spf

write -h -f verilog -o temp1_flt.v

# save design
#set filename [format "%s%s"  $my_toplevel ".ddc"]
# write -format ddc -hierarchy -output "cl_wrapper.ddc"

#start_gui

exit
