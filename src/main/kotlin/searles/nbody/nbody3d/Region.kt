package searles.nbody.nbody3d

data class Region(var center: Vec, var len: Double) {
    fun isInside(pt: Vec): Boolean {
        return pt.manhattanDistance(center) <= len
    }

    fun enlargeTowards(vec: Vec) {
        while(!isInside(vec)) {
            // opposite of 'shrink'
            if(vec.x <= center.x) center.x -= len
            else center.x += len

            if(vec.y <= center.y) center.y -= len
            else center.y += len

            if(vec.z <= center.z) center.z -= len
            else center.z += len

            len *= 2
        }
    }

    fun setTo(region: Region) {
        this.center.setTo(region.center)
        this.len = region.len
    }

    fun shrink(pt: Vec) {
        // shrink st it still contains pt.
        require(isInside(pt))

        len /= 2
        center.x = if(pt.x <= center.x) center.x - len else center.x + len
        center.y = if(pt.y <= center.y) center.y - len else center.y + len
        center.z = if(pt.z <= center.z) center.z - len else center.z + len

        require(isInside(pt))
    }

    fun indexOf(pt: Vec): Int {
        require(isInside(pt))

        return when {
            pt.x <= center.x && pt.y <= center.y && pt.z <= center.z -> 0
            pt.x > center.x && pt.y <= center.y && pt.z <= center.z -> 1
            pt.x > center.x && pt.y > center.y && pt.z <= center.z -> 2
            pt.x <= center.x && pt.y > center.y && pt.z <= center.z -> 3
            pt.x <= center.x && pt.y <= center.y && pt.z > center.z -> 4
            pt.x > center.x && pt.y <= center.y && pt.z > center.z -> 5
            pt.x > center.x && pt.y > center.y && pt.z > center.z -> 6
            else -> 7
        }
    }

    fun shrinkForDistinctChildren(pt0: Vec, pt1: Vec) {
        while(true) {
            val index0 = indexOf(pt0)
            val index1 = indexOf(pt1)

            if(index0 != index1) {
                break
            }

            // There is a very small chance of rounding errors in the last double-digit.
            len /= 2

            when(index0) {
                0 -> { center.x -= len; center.y -= len; center.z -= len }
                1 -> { center.x += len; center.y -= len; center.z -= len }
                2 -> { center.x += len; center.y += len; center.z -= len }
                3 -> { center.x -= len; center.y += len; center.z -= len }
                4 -> { center.x -= len; center.y -= len; center.z += len }
                5 -> { center.x += len; center.y -= len; center.z += len }
                6 -> { center.x += len; center.y += len; center.z += len }
                else -> { center.x -= len; center.y += len; center.z += len }
            }
        }
    }
}