cmake_minimum_required(VERSION 3.22.1)
project(sims2_core_bridge LANGUAGES CXX)

add_library(sims2_core_bridge SHARED sims2_core_bridge.cpp)

target_compile_features(sims2_core_bridge PRIVATE cxx_std_17)
target_compile_options(sims2_core_bridge PRIVATE
    -O3
    -Wall
    -Wextra
    -Wpedantic
    -fstack-protector-strong
)
target_link_options(sims2_core_bridge PRIVATE -Wl,-z,relro -Wl,-z,now)
find_library(log-lib log)
target_link_libraries(sims2_core_bridge PRIVATE ${log-lib})
