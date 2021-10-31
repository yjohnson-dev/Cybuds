package com.yjohnson.backend.entities.Group;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(path = "/groups")
public class GroupController {

	private final GroupRepository groupRepository;

	public GroupController(GroupRepository groupRepository) {
		this.groupRepository = groupRepository;
	}

	/**
	 * Adds a given group to the database. This method sanitizes the input to a degree; the fields will be trimmed and names will be capitalized in
	 * Title case (e.g. "marTHa" -> "Martha").
	 * <p>
	 * If the given object contains repeated unique fields, then those fields are returned alongside a CONFLICT status code.
	 *
	 * @param newGroupEntity the {@code GroupEntity} object to insert into the database.
	 *
	 * @return a response entity with the created {@code GroupEntity} object (CREATED) or the conflicting value (CONFLICT).
	 */
	@PostMapping
	public ResponseEntity<?> addGroup(@RequestBody GroupEntity newGroupEntity) {
		if (newGroupEntity.getName() == null || newGroupEntity.getName().isEmpty()) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		newGroupEntity.setName(StringUtils.trimWhitespace(StringUtils.capitalize(newGroupEntity.getName())));
		newGroupEntity.setDescription(StringUtils.trimWhitespace(StringUtils.capitalize(newGroupEntity.getDescription())));
		if (groupRepository.findByName(newGroupEntity.getName()).isPresent()) {              //1
			return new ResponseEntity<>(newGroupEntity.getName(), HttpStatus.CONFLICT);
		} else {
			return new ResponseEntity<>(groupRepository.save(newGroupEntity), HttpStatus.CREATED);
		}
	}

	/*
	Mappings are complicated when you want multiple path variable types.
	https://stackoverflow.com/questions/52260551/spring-boot-rest-single-path-variable-which-takes-different-type-of-values
	 */
	/**
	 * Retrieves a {@code GroupEntity} from the database whose ID matches the given path variable. 
	 * @param id       the id of the group to retrieve
	 *
	 * @return the {@code GroupEntity} object that corresponds with the path variable (OK) or an empty body (BAD REQUEST or NOT FOUND).
	 */
	@GetMapping(value = "/{identifier:[0-9]+}")
	public ResponseEntity<?> getGroupById(@PathVariable("identifier") Optional<Long> id) {
		Optional<GroupEntity> optionalGroup;
		if (id.isPresent()) optionalGroup = groupRepository.findById(id.get());     // 1
		else return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		return optionalGroup.map(groupEntity -> new ResponseEntity<>(groupEntity, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(
				HttpStatus.NOT_FOUND));
	}

	/**
	 * Retrieves a {@code GroupEntity} from the database whose name matches the given path variable.
	 *
	 * @param name       the name of the group to retrieve
	 *
	 * @return the {@code GroupEntity} object that corresponds with the path variable (OK) or an empty body (BAD REQUEST or NOT FOUND).
	 */
	@GetMapping(value = "/{identifier:[A-Za-z]+}")
	public ResponseEntity<?> getGroupByName(@PathVariable(name = "identifier") Optional<String> name) {
		Optional<GroupEntity> optionalGroup;
		if (name.isPresent()) optionalGroup = groupRepository.findByName(name.get());     // 1
		else return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		return optionalGroup.map(groupEntity -> new ResponseEntity<>(groupEntity, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(
				HttpStatus.NOT_FOUND));
	}

	/**
	 * Deletes the {@code GroupEntity} that corresponds to the given ID from the database.
	 *
	 * @param id the ID of the {@code GroupEntity} to be deleted.
	 *
	 * @return a response entity with the deleted {@code GroupEntity} object (OK) or an empty body (BAD REQUEST or NOT FOUND).
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<GroupEntity> deleteGroup(@PathVariable Optional<Long> id) {
		if (id.isPresent()) {
			try {
				Optional<GroupEntity> optionalGroup = groupRepository.findById(id.get());  //1
				if (optionalGroup.isPresent()) {
					GroupEntity deleted = optionalGroup.get().clone();
					groupRepository.delete(optionalGroup.get());                         //2
					return new ResponseEntity<>(deleted, HttpStatus.OK);
				}
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Updates the {@code GroupEntity} that corresponds to the given ID path variable with the values of the request body. It is not possible to update the
	 * ID or grouped users via this method. Values that do not need to be updated can be omitted.
	 *
	 * @param valuesToUpdate a {@code GroupEntity}-like JSON that holds the values to update.
	 * @param id             the ID of the {@code GroupEntity} to be updated.
	 *
	 * @return a response entity with the updated {@code GroupEntity} object (OK) or an empty body (BAD REQUEST, CONFLICT, or NOT FOUND).
	 */
	@PutMapping("/{id}")
	public ResponseEntity<GroupEntity> updateGroup(@RequestBody Optional<GroupEntity> valuesToUpdate, @PathVariable Optional<Long> id) {
		try {
			if (valuesToUpdate.isPresent() && id.isPresent()) {
				Optional<GroupEntity> fromDB = groupRepository.findById(id.get());     // 1
				return fromDB.map(group -> new ResponseEntity<>(
						groupRepository.save(group.updateContents(valuesToUpdate.get())),  // 2
						HttpStatus.OK
				)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
			}
		} catch (DataIntegrityViolationException e) {
			return new ResponseEntity<>(HttpStatus.CONFLICT);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Retrieves all the {@code GroupEntity} objects in the database.
	 *
	 * @return all the groups in the database
	 */
	@GetMapping
	public Iterable<GroupEntity> retrieveAll() {
		return groupRepository.findAll();
	}
}