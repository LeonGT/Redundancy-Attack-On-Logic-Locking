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
set target_library "tsmc65.db"
set link_library "* dw_foundation.sldb tsmc65.db"
set synthetic_library "dw_foundation.sldb"

read_verilog ./test.v
current_design dut

# check internal DC representation for design consistency
check_design

# verifies timing setup is complete
check_timing

