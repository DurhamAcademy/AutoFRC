package frc.team6502.robot.auto.pathing

import edu.wpi.first.wpilibj.geometry.Rotation2d
import edu.wpi.first.wpilibj.geometry.Translation2d
import edu.wpi.first.wpilibj.trajectory.Trajectory
import kyberlib.math.units.extensions.feet
import frc.team6502.robot.RobotContainer  // test edit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Object to manage pathfinding functions.
 * Implements RTT* in order to find the most efficient route
 * @author TateStaples
 */
object PathPlanner {
    val field = RobotContainer.navigation.field // test edit
    val tree = Tree()
    private val random = Random(4)//Timer.getFPGATimestamp().toInt())

    var minGoalDistance = 0.5.feet.value  // margin of error for pathfinding node
    var pathFound = false  // whether the Planner currently has a working path
    var endNode: Node? = null  // the node the represents the end goal [robot position] (think about changing to growing 2 seperate trees)
    val path: ArrayList<Node>?  // the working path of points to get from robot position to target goal
        get() = endNode?.let { tree.trace(it) }

    val explorationDepth = 5000  // how many nodes to create before giving up finding target
    val optimizationDepth = 100  // how many nodes to dedicate to optimization

    /**
     * Generates a trajectory to get from current estimated pose to a separate target
     * @param pose2d the pose that you want the robot to get to
     * @return a trajectory that will track your robot to the goal target
     */
    fun pathTo(position: Translation2d): Trajectory {
        if (tree.nodeCount > 0)
            tree.pruneInformed()
        loadTree(position, RobotContainer.navigation.position)  // test edit
        // TODO: figure out how to maintain rotation information
        return treeToTrajectory()
    }

    /**
     * Updates a trajectory when obstacles move
     * @param trajectory the old trajectory that may need correction
     * @return a trajectory that won't collide with any of the updated obstacles
     */
    fun updateTrajectory(trajectory: Trajectory): Trajectory {
        tree.pruneBlocked()
        if (tree.vertices.contains(endNode!!)) return trajectory
        return pathTo(trajectory.states.last().poseMeters.translation)
    }

    /**
     * Converts a navigation tree into a path for the robot to follow
     * @return a trajectory that follows the Tree recommended path
     */
    private fun treeToTrajectory(): Trajectory {
        val positions = ArrayList<Translation2d>()
        for (node in path!!) positions.add(node.position)
        return RobotContainer.navigation.trajectory(positions)  // test edit
    }

    /**
     * Creates the initial tree of nodes
     */
    internal fun loadTree(startPosition: Translation2d, endPosition: Translation2d) {  // to allow dynamic movement, maybe startPoint = goal and end is robot
        // look @ BIT*
        // current version is Informed RRT*
        pathFound = false
        Information.setup(startPosition, endPosition)
        tree.addNode(Node(startPosition))  // TODO: this may cause errors when loading tree several times
        for (i in tree.nodeCount..explorationDepth) {
            if (pathFound) break
            val point = randomPoint()
            addPoint(point)
        }
        for (i in tree.vertices.count { it.informed }..optimizationDepth)
            addPoint(informedPoint())
    }

    private fun addPoint(point: Translation2d) {
        val nearest = tree.nearestNode(point)!!  // asserts not null
        var delta = point.minus(nearest.position)
        val magnitude = delta.getDistance(Translation2d(0.0, 0.0))
        if (magnitude > tree.maxBranchLength) {
            delta = delta.times(tree.maxBranchLength/magnitude)  // resize the vector
        }
        val new = nearest.position.plus(delta)
        if (!field.inField(new)) {
            return
        }
        val node = Node(new, nearest)
        tree.addNode(node)
        tree.optimize(node)
        val endDis = new.getDistance(Information.endPosition)
        if (endDis < minGoalDistance && !(pathFound && endDis < path!!.first().pathLengthFromRoot)) {
            pathFound = true
            endNode = node
            Information.update()
            println("path found")
        }
    }

    /**
     * Generates random point in the field
     * @return a valid position in the field
     */
    fun randomPoint(): Translation2d {
        var x: Double
        var y: Double
        do {
            x = random.nextDouble(field.width)
            y = random.nextDouble(field.width)
        } while(!field.inField(x, y))  // the convertions here might cause issues
        return Translation2d(x, y)
    }

    /**
     * Modified random sample that chooses from inside an oval.
     * This is done because once a rough path is found, no nodes outside the oval can improve the path
     */
    private fun informedPoint(): Translation2d {
        val theta = random.nextDouble(2*Math.PI)
        val rho = random.nextDouble(1.0)
        return Information.get(rho, theta)
    }

    /**
     * Illustrate to tree of values
     */
    private fun drawTreePath() {
        // TODO: update with field stuff and smartdashboard
        tree.draw()
    }

    /**
     * Information regard what sample of points can be used to further optimize the current path
     */
    object Information {
        lateinit var startPosition: Translation2d
        lateinit var endPosition: Translation2d
        lateinit var center: Translation2d
        var currentPathLength = 0.0
        var dis = 0.0
        var width = currentPathLength
        var height = 0.0
        lateinit var shifted: Translation2d
        lateinit var rotation: Rotation2d

        fun setup(startPosition: Translation2d, endPosition: Translation2d) {
            this.startPosition = startPosition
            this.endPosition = endPosition
            center = startPosition.plus(endPosition).div(2.0)  // average
            dis = startPosition.getDistance(endPosition)
            shifted = endPosition.minus(startPosition)
            rotation = Rotation2d(shifted.x, shifted.y)
        }

        fun update() {
            currentPathLength = path!![0].pathLengthFromRoot
            width = currentPathLength
            height = sqrt(currentPathLength * currentPathLength - dis * dis)
        }

        fun get(rho: Double, theta: Double): Translation2d {
            val x = cos(theta) * width/2 * rho
            val y = sin(theta) * height/2 * rho
            return Translation2d(x, y).rotateBy(rotation).plus(center)
        }
    }
}