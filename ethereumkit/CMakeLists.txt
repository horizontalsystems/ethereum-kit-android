# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

OPTION(FAST_MATH "Math optimizations" OFF)
OPTION(ETH_NANO "build minimal eth verification.(eth_getTransactionReceipt)" ON)
OPTION(ETH_BASIC "build basic eth verification.(all rpc-calls except eth_call)" ON)
OPTION(ETH_FULL "build full eth verification.(including eth_call)" ON)
OPTION(IN3API "build the USN-API which offer better interfaces and additional functions on top of the pure verification" ON)

IF(FAST_MATH)
  MESSAGE ( STATUS "Enable math optimizations (excutable size may increase)" )
  ADD_DEFINITIONS(-DIN3_MATH_FAST)
ELSE()
  MESSAGE ( STATUS "Disable math optimizations (optimised for executable size)" )
  ADD_DEFINITIONS(-DIN3_MATH_LITE)
ENDIF(FAST_MATH)

ADD_DEFINITIONS(
  -DUSE_PRECOMPUTED_CP=1
  -DERR_MSG
  -DETH_FULL
  -DETH_BASIC
  -DETH_NANO
  -DIN3_EXPORT_TEST=static
)
SET(IN3_VERIFIER eth_full)

if(IN3API)
  ADD_DEFINITIONS(-DETH_API)
  set(IN3_API eth_api)
endif()


foreach(module core verifier/eth1/nano verifier/eth1/evm verifier/eth1/basic verifier/eth1/full ../../java/src third-party/crypto third-party/tommath api)
        file(MAKE_DIRECTORY in3-c/c/src/${module}/outputs)
        add_subdirectory(
                in3-c/c/src/${module}
                in3-c/c/src/${module}/outputs)
endforeach()