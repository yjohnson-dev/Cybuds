package com.yjohnson.backend.entities.User;

import com.yjohnson.backend.entities.DB_Relations.R_UserGroup;
import com.yjohnson.backend.entities.DB_Relations.R_UserInterest;
import com.yjohnson.backend.entities.DB_Relations.UserGroupRepository;
import com.yjohnson.backend.entities.DB_Relations.UserInterestRepository;
import com.yjohnson.backend.entities.Group.GroupEntity;
import com.yjohnson.backend.entities.Group.GroupRepository;
import com.yjohnson.backend.entities.Interest.InterestEntity;
import com.yjohnson.backend.entities.Interest.InterestRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.yjohnson.backend.entities.DB_Relations.R_UserInterest;
import com.yjohnson.backend.entities.Group.GroupEntity;
import com.yjohnson.backend.entities.Group.GroupType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping(path = "/users")
public class UserController {
	private final GroupRepository groupRepository;
	private final InterestRepository interestRepository;
	private final UserRepository userRepository;
	private final UserGroupRepository userGroupRepository;
	private final UserInterestRepository userInterestRepository;
	private final UserService userService;

	public UserController(UserRepository userRepository, UserGroupRepository userGroupRepository, GroupRepository groupRepository, InterestRepository interestRepository, UserInterestRepository userInterestRepository) {
		this.userRepository = userRepository;
		this.userGroupRepository = userGroupRepository;
		this.groupRepository = groupRepository;
		this.interestRepository = interestRepository;
		this.userInterestRepository = userInterestRepository;
		this.userService = new UserService(userRepository);
	}

	/**
	 * Adds a given user to the database. This method sanitizes the input to a degree; the fields will be trimmed, names will be capitalized in
	 * Title case (e.g. "marTHa" -> "Martha"), the username and email will be lowercase and the phone number will be reduced to a max of 10 digits.
	 * <p>
	 * If the given object contains repeated unique fields, then those fields are returned alongside a CONFLICT status code.
	 *
	 * @param newUser the user object to insert into the database.
	 *
	 * @return a response entity with the created {@code User} object (CREATED) or the conflicting value (CONFLICT).
	 */
	@PostMapping()
	public ResponseEntity<?> addUser(@RequestBody User newUser) {
		newUser.setFirstName    (StringUtils.trimWhitespace(StringUtils.capitalize(newUser.getFirstName().toLowerCase())));
		newUser.setMiddleName   (StringUtils.trimWhitespace(StringUtils.capitalize(newUser.getMiddleName().toLowerCase())));
		newUser.setLastName     (StringUtils.trimWhitespace(StringUtils.capitalize(newUser.getLastName().toLowerCase())));
		newUser.setUsername     (StringUtils.trimAllWhitespace(newUser.getUsername().toLowerCase()));
		newUser.setEmail        (StringUtils.trimAllWhitespace(newUser.getEmail().toLowerCase()));
		newUser.setPhoneNumber  (StringUtils.deleteAny(newUser.getPhoneNumber(), "-()/_-+ ").substring(0,10));

		if (userRepository.findUserByEmail(newUser.getEmail()).isPresent()) {               // 1
			return new ResponseEntity<>(newUser.getEmail(), HttpStatus.CONFLICT);
		} else if (userRepository.findUserByUsername(newUser.getUsername()).isPresent()) {  // 2
			return new ResponseEntity<>(newUser.getUsername(), HttpStatus.CONFLICT);
		} else {
			return new ResponseEntity<>(userRepository.save(newUser), HttpStatus.CREATED);  // 3
		}
	}


	/**
	 * Retrieves a {@code User} from the database whose ID or username matches the given path variable. Only one of the two is required.
	 *
	 * @param identifier the id or username of the user to retrieve
	 *
	 * @return the {@code User} object that corresponds with the path variable (OK) or an empty body (BAD REQUEST or NOT FOUND).
	 */
	@Operation(summary = "Get a user by its ID or username")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Got the user", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = User.class))
			}),
			@ApiResponse(responseCode = "400", description = "Missing parameter"),
			@ApiResponse(responseCode = "404", description = "User not found")
	})
	@GetMapping("/{identifier}")
	public ResponseEntity<?> getUser(@PathVariable Optional<String> identifier) {
		Optional<User> optionalUser;
		if (identifier.isPresent()) {
			optionalUser = userService.getUser(identifier.get());
		} else return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

		// If it is a valid identifier then return the user associated with it
		return optionalUser.map(user -> new ResponseEntity<>(user, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}


	/**
	 * Deletes the {@code User} that corresponds to the given ID from the database.
	 *
	 * @param id the ID of the {@code User} to be deleted.
	 *
	 * @return a response entity with the deleted {@code User} object (OK) or an empty body (BAD REQUEST or NOT FOUND).
	 */
	@Operation(summary = "Delete a user by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Deleted the user", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = User.class))
			}),
			@ApiResponse(responseCode = "400", description = "Missing parameter"),
			@ApiResponse(responseCode = "404", description = "User not found"),
			@ApiResponse(responseCode = "500", description = "Unrecoverable exception occurred"),
	})
	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteUser(@PathVariable Optional<Long> id) {
		if (id.isPresent()) {
			try {
				Optional<User> result = userService.deleteUserByID(id.get());
				return result.map(user -> new ResponseEntity<>(user, HttpStatus.OK))
				             .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}


	/**
	 * Updates the {@code User} that corresponds to the given ID path variable with the values of the request body. It is not possible to update the
	 * ID, group relations, or interests via this method. Values that do not need to be updated can be omitted.
	 *
	 * @param valuesToUpdate a {@code User}-like JSON that holds the values to update.
	 * @param id             the ID of the {@code User} to be updated.
	 *
	 * @return a response entity with the updated {@code User} object (OK) or an empty body (BAD REQUEST, CONFLICT, or NOT FOUND).
	 */
	@Operation(summary = "Updates a user by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Updated the user", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = User.class))
			}),
			@ApiResponse(responseCode = "400", description = "Missing parameter"),
			@ApiResponse(responseCode = "404", description = "User not found"),
			@ApiResponse(responseCode = "409", description = "Update results in conflict"),
	})
	@PutMapping(value = "/{id}")
	public ResponseEntity<User> updateUser(@RequestBody Optional<User> valuesToUpdate, @PathVariable Optional<Long> id) {
		try {
			if (valuesToUpdate.isPresent() && id.isPresent()) {
				Optional<User> fromDB = userService.getUserByID(id.get());
				return fromDB.map(user -> new ResponseEntity<>(
						userService.saveUpdatedUser(valuesToUpdate.get(), user),  // 2
						HttpStatus.OK
				)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
			}
		} catch (DataIntegrityViolationException e) {
			return new ResponseEntity<>(HttpStatus.CONFLICT);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Retrieves all the {@code User} objects in the database.
	 *
	 * @return all the users in the database
	 */
	@Operation(summary = "Gets all users")
	@GetMapping
	public Iterable<User> getAllUsers() {
		return userService.getAllUsersFromDB();    //1
	}

	@Operation(summary = "Gets all groups for a given user")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Got user's groups", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = R_UserGroup.class))
			}),
			@ApiResponse(responseCode = "400", description = "Missing parameter"),
			@ApiResponse(responseCode = "404", description = "Not found"),
	})
	@GetMapping("/{id}/groups")
	public ResponseEntity<?> getAllGroupsForUser(@PathVariable Optional<Long> id) {
		if (id.isPresent()) {
			Optional<User> optionalUser = userService.getUserByID(id.get());
			return optionalUser.map(user -> new ResponseEntity<Iterable<R_UserGroup>>(user.getGroups(), HttpStatus.OK))
			                   .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	@Operation(summary = "Add a User-Group relation")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Added the group", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = R_UserGroup.class))
			}),
			@ApiResponse(responseCode = "400", description = "Missing parameter"),
			@ApiResponse(responseCode = "409", description = "Relation already exists")
	})
	@PostMapping("/{id}/groups/{gid}")
	public ResponseEntity<?> addRelation(@PathVariable("id") Optional<Long> user_id, @PathVariable("gid") Optional<Long> group_id) {
		/* Parameter Checking */
		if (!user_id.isPresent() || !group_id.isPresent()) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

		Optional<User> user = userService.getUserByID(user_id.get());
		Optional<GroupEntity> optionalGroup = groupRepository.findById(group_id.get());// 2

		if (!user.isPresent() || !optionalGroup.isPresent()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		if (userGroupRepository.findByUserAndGroup(user.get(), optionalGroup.get()).isPresent())
			return new ResponseEntity<>(HttpStatus.CONFLICT); // 3

		R_UserGroup relation = userGroupRepository.save(new R_UserGroup(user.get(), optionalGroup.get(), LocalDateTime.now())); //4
		user.get().getGroups().add(relation);
		optionalGroup.get().members.add(relation);
		userRepository.save(user.get()); // 5
		groupRepository.save(optionalGroup.get()); // 6
		return new ResponseEntity<>(
				relation,  // Updated all but ID.
				HttpStatus.CREATED
		);
	}

	@Operation(summary = "Delete a User-Group relation based off both their IDs")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Deleted the group", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = R_UserGroup.class))
			}),
			@ApiResponse(responseCode = "400", description = "Missing parameter"),
			@ApiResponse(responseCode = "404", description = "Not found"),
	})
	@DeleteMapping("/{id}/groups/{gid}")
	public ResponseEntity<?> deleteRelation(@PathVariable("id") Optional<Long> user_id, @PathVariable("gid") Optional<Long> group_id) {
		/* Parameter Checking */
		if (!user_id.isPresent() || !group_id.isPresent()) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

		Optional<User> user = userService.getUserByID(user_id.get());
		Optional<GroupEntity> optionalGroup = groupRepository.findById(group_id.get());// 2

		if (!user.isPresent() || !optionalGroup.isPresent()) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		Optional<R_UserGroup> relation = userGroupRepository.findByUserAndGroup(user.get(), optionalGroup.get());
		if (!relation.isPresent()) return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 3

		user.get().getGroups().remove(relation.get());
		optionalGroup.get().members.remove(relation.get());
		userGroupRepository.delete(relation.get()); //4
		userRepository.save(user.get()); // 5
		groupRepository.save(optionalGroup.get()); // 6
		return new ResponseEntity<>(
				relation,
				HttpStatus.OK
		);
	}

	@Operation(summary = "Gets all interests for a given user")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Got user's interests", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = R_UserInterest.class))
			}),
			@ApiResponse(responseCode = "400", description = "Missing parameter"),
			@ApiResponse(responseCode = "404", description = "Not found"),
	})
	@GetMapping("/{id}/interests")
	public ResponseEntity<?> getAllInterestsForUser(@PathVariable Optional<Long> id) {
		if (id.isPresent()) {
			Optional<User> optionalUser = userService.getUserByID(id.get());
			return optionalUser.map(user -> new ResponseEntity<Iterable<R_UserInterest>>(user.getInterests(), HttpStatus.OK))
			                   .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	@Operation(summary = "Add a User-Interest relation")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Added the interest", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = R_UserInterest.class))
			}),
			@ApiResponse(responseCode = "400", description = "Missing parameter"),
			@ApiResponse(responseCode = "409", description = "Relation already exists")
	})
	@PostMapping("/{id}/interests/{gid}")
	public ResponseEntity<?> addInterestRelation(@PathVariable("id") Optional<Long> user_id, @PathVariable("gid") Optional<Long> interest_id) {
		/* Parameter Checking */
		if (!user_id.isPresent() || !interest_id.isPresent()) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

		Optional<User> user = userService.getUserByID(user_id.get());
		Optional<InterestEntity> optionalInterest = interestRepository.findById(interest_id.get());// 2

		if (!user.isPresent() || !optionalInterest.isPresent()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		if (userInterestRepository.findByUserAndInterest(user.get(), optionalInterest.get()).isPresent())
			return new ResponseEntity<>(HttpStatus.CONFLICT); // 3

		R_UserInterest relation = userInterestRepository.save(new R_UserInterest(user.get(), optionalInterest.get(), LocalDateTime.now())); //4
		user.get().getInterests().add(relation);
		optionalInterest.get().interested.add(relation);
		userRepository.save(user.get()); // 5
		interestRepository.save(optionalInterest.get()); // 6
		return new ResponseEntity<>(
				relation,  // Updated all but ID.
				HttpStatus.CREATED
		);
	}

	@Operation(summary = "Delete a User-Interest relation based off both their IDs")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Deleted the interest", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = R_UserInterest.class))
			}),
			@ApiResponse(responseCode = "400", description = "Missing parameter"),
			@ApiResponse(responseCode = "404", description = "Not found"),
	})
	@DeleteMapping("/{id}/interests/{gid}")
	public ResponseEntity<?> deleteInterestRelation(@PathVariable("id") Optional<Long> user_id, @PathVariable("gid") Optional<Long> interest_id) {
		/* Parameter Checking */
		if (!user_id.isPresent() || !interest_id.isPresent()) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

		Optional<User> user = userService.getUserByID(user_id.get());
		Optional<InterestEntity> optionalInterest = interestRepository.findById(interest_id.get());// 2

		if (!user.isPresent() || !optionalInterest.isPresent()) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		Optional<R_UserInterest> relation = userInterestRepository.findByUserAndInterest(user.get(), optionalInterest.get());
		if (!relation.isPresent()) return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 3

		user.get().getInterests().remove(relation.get());
		optionalInterest.get().interested.remove(relation.get());
		userInterestRepository.delete(relation.get()); //4
		userRepository.save(user.get()); // 5
		interestRepository.save(optionalInterest.get()); // 6
		return new ResponseEntity<>(
				relation,
				HttpStatus.OK
		);
	}

	/**
	 * Sorts all user profiles that have the same characteristic as the choice of the current user.
	 * Calls add() which counts the number of characterists that are shared.
	 * Returns user with the most shared characteristics.
	 *
	 * @param id user id
	 * @param choice  the identifier the current user wanted to match with
	 * @return user profile with the most in common with current user
	 */
	@GetMapping("/{id}/match")
	public ResponseEntity<?> match(@PathVariable Optional<Long> id, @RequestBody GroupType choice) {
		if (id.isPresent()) {
			Optional<User> current = userRepository.findById(id.get());
			if (current.isPresent()) {
				int same = 0;
				int t=0;
				User temp = null;
				Iterable<User> All = getAllUsers();
				for (User Bob : All) {
					if (Bob.getId() != current.get().getId()) {

						switch (choice) {
							case STUDENT_CLASS:
								if (Bob.classification == current.get().classification){
									t = add(current.get(), Bob);
								}
								break;

							case COLLEGE:
								Iterable<GroupEntity> college1 = current.get().getColleges();
								Iterable<GroupEntity> college2 = Bob.getColleges();
								for (GroupEntity c1 : college1) {
									for (GroupEntity c2 : college2) {
										if (c1 == c2) {
											t = add(current.get(), Bob);
										}
									}
								}
								break;

							case STUDENT_MAJOR:
								Iterable<GroupEntity> major1 = current.get().getMajors();
								Iterable<GroupEntity> major2 = Bob.getMajors();
								for (GroupEntity g1 : major1) {
									for (GroupEntity g2 : major2) {
										if (g1 == g2) {
											t = add(current.get(), Bob);
										}
									}
								}
								break;

							default:
								break;
						}


						if (t > same) {
							same = t;
							temp = Bob;
						}
					}
				}            return new ResponseEntity<>(temp,HttpStatus.OK);

			}
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);

		}            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

	}


	/**
	 * Helper method tracks the number of characteristics that are the same between two users.
	 * @param one user one
	 * @param two, user two
	 * @return, returns int of the number of shared characteristics
	 */
	public int add (User one, User two){
		int i = 0;
		//iterate through all interests-sees if any match
		for (R_UserInterest r1 : one.interestedIn) {
			for (R_UserInterest r2 : two.interestedIn) {
				if (r1 == r2) ++i;
			}
		}
		//iterate through all majors-counts if any match
		Iterable<GroupEntity> major1 = one.getMajors();
		Iterable<GroupEntity> major2 = two.getMajors();
		for (GroupEntity g1 : major1) {
			for (GroupEntity g2 : major2) {
				if (g1 == g2) ++i;
			}
		}

		//iterate through all colleges-counts if any match
		Iterable<GroupEntity> college1 = one.getColleges();
		Iterable<GroupEntity> college2 = two.getColleges();
		for (GroupEntity c1 : college1) {
			for (GroupEntity c2 : college2) {
				if (c1 == c2) ++i;
			}
		}

		//another point if the students are the same class year
		if (one.classification == two.classification) {
			i++;
		}

		return i;
	}
}