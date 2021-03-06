package com.yjohnson.backend.entities;

import com.yjohnson.backend.entities.User.User;
import com.yjohnson.backend.entities.User.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping()
public class GeneralController {
	private final UserRepository userRepository;

	public GeneralController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * Sends the user object that matches the attemptedLogin object. It queries the database for the unique user with the provided email; if no email
	 * was provided, then it searches for the unique user with the provided username.
	 * <p>
	 * When the queried user is present and their password hash matches the provided one, the User object is returned as the response body. Otherwise,
	 * a 404 is returned instead.
	 *
	 * @param attemptedLogin the pseudo-user object that contains either the email or username and a password hash
	 *
	 * @return the User that corresponds to that object, 404 otherwise
	 */
	@PostMapping("/login")
	public ResponseEntity<User> stageLogin(@RequestBody User attemptedLogin) {
		Optional<User> query = userRepository.findByEmail(attemptedLogin.getEmail());        // 1
		if (query.isPresent() && Objects.equals(query.get().getPasswordHash(), attemptedLogin.getPasswordHash())) {
			return new ResponseEntity<>(query.get(), HttpStatus.OK);
		} else {
			query = userRepository.findByUsername(attemptedLogin.getUsername());                   // 2
			if (query.isPresent() && Objects.equals(query.get().getPasswordHash(), attemptedLogin.getPasswordHash())) {
				return new ResponseEntity<>(query.get(), HttpStatus.OK);
			} else {
				if (query.isPresent() && query.get().getPasswordHash().isEmpty()) return new ResponseEntity<>(attemptedLogin, HttpStatus.BAD_REQUEST);
				else return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		}
	}

	/**
	 * Adds a given user to the database. This method sanitizes the input to a degree; the fields will be trimmed, names will be capitalized in Title
	 * case (e.g. "marTHa" - "Martha"), the username and email will be lowercase and the phone number will be reduced to a max of 10 digits.
	 * <p>
	 * If the given object contains repeated unique fields, then those fields are returned alongside a CONFLICT status code.
	 *
	 * @param optionalUser the user object to insert into the database.
	 *
	 * @return a response entity with the created {@code User} object (CREATED) or the conflicting value (CONFLICT).
	 */
	@PostMapping("/register")
	public ResponseEntity<?> stageRegistration(@RequestBody Optional<User> optionalUser) {
		try {
			if (optionalUser.isPresent()) {
				User user = optionalUser.get();
				if (user.validate()) {
					user.setFirstName(StringUtils.trimWhitespace(StringUtils.capitalize(user.getFirstName().toLowerCase())));
					if (user.getMiddleName() != null) {
						user.setMiddleName(StringUtils.trimWhitespace(StringUtils.capitalize(user.getMiddleName().toLowerCase())));
					}
					user.setLastName(StringUtils.trimWhitespace(StringUtils.capitalize(user.getLastName().toLowerCase())));
					user.setUsername(StringUtils.trimAllWhitespace(user.getUsername().toLowerCase()));
					user.setEmail(StringUtils.trimAllWhitespace(user.getEmail().toLowerCase()));
					if (user.getPhoneNumber() != null) user.setPhoneNumber(String.format(
							"%10s",
							StringUtils.deleteAny(
									user.getPhoneNumber(),
									"-()/_-+ "
							)
					));

					if (userRepository.findByEmail(user.getEmail()).isPresent()) {               // 1
						return new ResponseEntity<>(user.getEmail(), HttpStatus.CONFLICT);
					} else if (userRepository.findByUsername(user.getUsername()).isPresent()) {  // 2
						return new ResponseEntity<>(user.getUsername(), HttpStatus.CONFLICT);
					}
					return new ResponseEntity<>(userRepository.save(user), HttpStatus.CREATED);
				}
			}
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		} catch (DataIntegrityViolationException e) {
			return new ResponseEntity<>(HttpStatus.CONFLICT);
		}
	}
//
//	@GetMapping("/api/majors")
//	public ResponseEntity<?> retrieveMajors() {
//		List<String> list = new ArrayList<>();
//		URL url = this.getClass().getResource("/static/majors.txt");
//		try (Stream<String> stream = Files.lines(Paths.get(url.toURI()))) {
//			stream.forEach(list::add);
//			return new ResponseEntity<>(list, HttpStatus.OK);
//		} catch (IOException | URISyntaxException e) {
//			e.printStackTrace();
//			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}
//
//	@GetMapping("/api/colleges")
//	public ResponseEntity<?> retrieveColleges() {
//		List<String> list = new ArrayList<>();
//		URL url = this.getClass().getResource("/static/colleges.txt");
//		try (Stream<String> stream = Files.lines(Paths.get(url.toURI()))) {
//			stream.forEach(list::add);
//			return new ResponseEntity<>(list, HttpStatus.OK);
//		} catch (IOException | URISyntaxException e) {
//			e.printStackTrace();
//			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}
}