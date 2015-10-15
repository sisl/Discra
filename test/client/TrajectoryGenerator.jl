module TrajectoryGenerator

export Waypoint, Advisory, trajectory_generator


type Waypoint
    lat     :: Float64  # [dec deg] latitude
    lon     :: Float64  # [dec deg] longitude
    alt     :: Float64  # [ft AGL] altitude
    speed   :: Float64  # [ft/s] speed
    head    :: Float64  # [deg] heading
    period  :: Float64  # [s] period to hold maneuver parameters
    turn    :: Float64  # [deg/s] turn rate
end # type

typealias Advisory Vector{Waypoint}


function latlon2xy(lat::Float64, lon::Float64)
    #= Converts lat lon measurements from decimal degrees to ft. =#
    x = lat * (10000 / 90) * 3280.4  # [ft] East is positive
    y = lon * (10000 / 90) * 3280.4  # [ft] North is positive
    return [x, y]
end # function latlon2xy


function state_update!(wp::Waypoint, dt::Float64)
    #= Updates the state based on standard aircraft dynamics. =#
    if wp.turn != 0
        turn_radius = abs(wp.speed / deg2rad(wp.turn))
        turn_amount = deg2rad(wp.turn) * dt
        
        wp.lat += turn_radius * sign(turn_amount) * 
                  (sin(deg2rad(wp.head)) - sin(deg2rad(wp.head) - turn_amount))
        wp.lon += turn_radius * sign(turn_amount) * 
                  (-cos(deg2rad(wp.head)) + cos(deg2rad(wp.head) - turn_amount))
        wp.head += rad2deg(turn_amount)
    else
        wp.lat += wp.speed * dt * cos(deg2rad(wp.head))
        wp.lon += wp.speed * dt * sin(deg2rad(wp.head))
    end # if
end # function state_update!


function initialize_trajectory(adv::Advisory, dt::Float64)
    time_steps = int(sum([wp.period / dt + 1 for wp in adv]))
    traj = zeros(4, time_steps)
    return traj
end # function initialize_trajectory


function update_trajectory!(traj::Matrix{Float64}, counter::Int64, wp::Waypoint)
    traj[1, counter] = wp.lat
    traj[2, counter] = wp.lon
    traj[3, counter] = wp.speed
    traj[4, counter] = wp.head
end # function update_trajectory!


function trajectory_generator(adv::Advisory, dt::Float64)
    #= 
    Generates the flight path with the required speed and heading.
    |dt| controls the fineness of the plots, and can be changed for,
    say, an MPC-based trajectory-following algorithm.
    =#
    traj = initialize_trajectory(adv, dt)
    wp_curr = Waypoint(0., 0., 0., 0., 0., 0., 0.)
    counter = 1
    
    for i = 1:length(adv)
        wp_curr = deepcopy(adv[i])
        update_trajectory!(traj, counter, wp_curr)
        counter += 1
        
        for j = 1:adv[i].period / dt
            state_update!(wp_curr, dt)
            update_trajectory!(traj, counter, wp_curr)
            counter += 1
        end # for j
    end # for i
    
    return traj
end # function trajectory_generator

end # module