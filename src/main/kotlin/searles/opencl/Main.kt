import org.jocl.*


object ArrayGPU {
    /**
     * The source code of the OpenCL program
     */
    private const val programSource = "__kernel void " +
            "sampleKernel(__global const float *a," +
            "             __global const float *b," +
            "             __global float *c)" +
            "{" +
            "    int gid = get_global_id(0);" +
            "    c[gid] = a[gid] + b[gid];" +
            "}"

    @JvmStatic
    fun main(args: Array<String>) {
        val n = 10000000
        val srcArrayA = FloatArray(n)
        val srcArrayB = FloatArray(n)
        val dstArray = FloatArray(n)
        for (i in 0 until n) {
            srcArrayA[i] = i.toFloat()
            srcArrayB[i] = i.toFloat()
        }
        val srcA: Pointer = Pointer.to(srcArrayA)
        val srcB: Pointer = Pointer.to(srcArrayB)
        val dst: Pointer = Pointer.to(dstArray)


        // The platform, device type and device number
        // that will be used
        val platformIndex = 0
        val deviceType = CL.CL_DEVICE_TYPE_ALL
        val deviceIndex = 0

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true)

        // Obtain the number of platforms
        val numPlatformsArray = IntArray(1)
        CL.clGetPlatformIDs(0, null, numPlatformsArray)
        val numPlatforms = numPlatformsArray[0]

        // Obtain a platform ID
        val platforms = arrayOfNulls<cl_platform_id>(numPlatforms)
        CL.clGetPlatformIDs(platforms.size, platforms, null)
        val platform = platforms[platformIndex]

        // Initialize the context properties
        val contextProperties = cl_context_properties()
        contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM.toLong(), platform)

        // Obtain the number of devices for the platform
        val numDevicesArray = IntArray(1)
        CL.clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray)
        val numDevices = numDevicesArray[0]

        // Obtain a device ID
        val devices = arrayOfNulls<cl_device_id>(numDevices)
        CL.clGetDeviceIDs(platform, deviceType, numDevices, devices, null)
        val device = devices[deviceIndex]

        // Create a context for the selected device
        val context = CL.clCreateContext(
            contextProperties, 1, arrayOf(device),
            null, null, null
        )

        // Create a command-queue for the selected device
        val commandQueue = CL.clCreateCommandQueue(context, device, 0, null)

        // Allocate the memory objects for the input and output data
        val memObjects = arrayOfNulls<cl_mem>(3)
        memObjects[0] = CL.clCreateBuffer(
            context,
            CL.CL_MEM_READ_ONLY or CL.CL_MEM_COPY_HOST_PTR,
            (
                    Sizeof.cl_float * n).toLong(), srcA, null
        )
        memObjects[1] = CL.clCreateBuffer(
            context,
            CL.CL_MEM_READ_ONLY or CL.CL_MEM_COPY_HOST_PTR,
            (
                    Sizeof.cl_float * n).toLong(), srcB, null
        )
        memObjects[2] = CL.clCreateBuffer(
            context,
            CL.CL_MEM_READ_WRITE,
            (
                    Sizeof.cl_float * n).toLong(), null, null
        )

        // Create the program from the source code
        val program = CL.clCreateProgramWithSource(
            context,
            1, arrayOf(programSource), null, null
        )

        // Build the program
        CL.clBuildProgram(program, 0, null, null, null, null)

        // Create the kernel
        val kernel = CL.clCreateKernel(program, "sampleKernel", null)

        // Set the arguments for the kernel
        CL.clSetKernelArg(
            kernel, 0,
            Sizeof.cl_mem.toLong(), Pointer.to(memObjects[0])
        )
        CL.clSetKernelArg(
            kernel, 1,
            Sizeof.cl_mem.toLong(), Pointer.to(memObjects[1])
        )
        CL.clSetKernelArg(
            kernel, 2,
            Sizeof.cl_mem.toLong(), Pointer.to(memObjects[2])
        )

        // Set the work-item dimensions
        val global_work_size = longArrayOf(n.toLong())
        val local_work_size = longArrayOf(1)

        // Execute the kernel
        CL.clEnqueueNDRangeKernel(
            commandQueue, kernel, 1, null,
            global_work_size, local_work_size, 0, null, null
        )

        // Read the output data
        CL.clEnqueueReadBuffer(
            commandQueue, memObjects[2], CL.CL_TRUE, 0,
            (n * Sizeof.cl_float).toLong(), dst, 0, null, null
        )

        // Release kernel, program, and memory objects
        CL.clReleaseMemObject(memObjects[0])
        CL.clReleaseMemObject(memObjects[1])
        CL.clReleaseMemObject(memObjects[2])
        CL.clReleaseKernel(kernel)
        CL.clReleaseProgram(program)
        CL.clReleaseCommandQueue(commandQueue)
        CL.clReleaseContext(context)
    }

    private fun getString(device: cl_device_id, paramName: Int): String {
        // Obtain the length of the string that will be queried
        val size = LongArray(1)
        CL.clGetDeviceInfo(device, paramName, 0, null, size)

        // Create a buffer of the appropriate size and fill it with the info
        val buffer = ByteArray(size[0].toInt())
        CL.clGetDeviceInfo(device, paramName, buffer.size.toLong(), Pointer.to(buffer), null)

        // Create a string from the buffer (excluding the trailing \0 byte)
        return String(buffer, 0, buffer.size - 1)
    }
}