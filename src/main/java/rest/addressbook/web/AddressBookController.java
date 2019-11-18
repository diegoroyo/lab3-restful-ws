package rest.addressbook.web;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import rest.addressbook.domain.AddressBook;
import rest.addressbook.domain.Person;

/**
 * A service that manipulates contacts in an address book.
 */
@Path("/contacts")
public class AddressBookController {

  /**
   * The (shared) address book object.
   */
  @Inject
  AddressBook addressBook;

  private CacheControl getCacheControl() {
    CacheControl cc = new CacheControl();
    cc.setMaxAge(86400);
    cc.setPrivate(true);
    return cc;
  }

  /**
   * A GET /contacts request should return the address book in JSON.
   *
   * @return a JSON representation of the address book.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAddressBook(@Context Request request) {
    EntityTag etag = new EntityTag(Integer.toString(addressBook.hashCode()));
    ResponseBuilder builder = request.evaluatePreconditions(etag);
    if (builder == null) {
      return Response.ok(addressBook).cacheControl(getCacheControl()).tag(etag).build();
    } else {
      return builder.build();
    }
  }

  /**
   * A POST /contacts request should add a new entry to the address book.
   *
   * @param info   the URI information of the request
   * @param person the posted entity
   * @return a JSON representation of the new entry that should be available at
   * /contacts/person/{id}.
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addPerson(@Context UriInfo info, Person person) {
    addressBook.getPersonList().add(person);
    person.setId(addressBook.nextId());
    person.setHref(info.getAbsolutePathBuilder().path("person/{id}").build(person.getId()));
    return Response.created(person.getHref()).entity(person).build();
  }

  /**
   * A GET /contacts/person/{id} request should return a entry from the address book
   *
   * @param id the unique identifier of a person
   * @return a JSON representation of the new entry or 404
   */
  @GET
  @Path("/person/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPerson(@Context Request request,
                            @PathParam("id") int id) {
    for (Person p : addressBook.getPersonList()) {
      if (p.getId() == id) {
        EntityTag etag = new EntityTag(Integer.toString(p.hashCode()));
        ResponseBuilder builder = request.evaluatePreconditions(etag);
        if (builder == null) {
          return Response.ok(p).cacheControl(getCacheControl()).tag(etag).build();
        } else {
          return builder.build();
        }
      }
    }
    return Response.status(Status.NOT_FOUND).build();
  }

  /**
   * A PUT /contacts/person/{id} should update a entry if exists
   *
   * @param info   the URI information of the request
   * @param person the posted entity
   * @param id     the unique identifier of a person
   * @return a JSON representation of the new updated entry or 400 if the id is not a key
   */
  @PUT
  @Path("/person/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updatePerson(@Context Request request,
                               @HeaderParam("If-Match") String ifMatch,
                               @Context UriInfo info,
                               @PathParam("id") int id, Person person) {
    for (int i = 0; i < addressBook.getPersonList().size(); i++) {
      Person p = addressBook.getPersonList().get(i);
      if (p.getId() == id) {
        EntityTag etag = new EntityTag(Integer.toString(p.hashCode()));
        ResponseBuilder builder = request.evaluatePreconditions(etag);
        System.out.println("etag calculado: " + etag);
        // client is not up to date (send back 412)
        if (builder != null) {
            return builder.build();
        }
        person.setId(id);
        person.setHref(info.getAbsolutePath());
        if (ifMatch != null && p.equals(person)) {
          // no need to update content, respond with 204
          return Response.noContent().build();
        } else {
          // etag is the same but needs to update
          addressBook.getPersonList().set(i, person);
          EntityTag newTag = new EntityTag(Integer.toString(person.hashCode()));
          return Response.ok(person).cacheControl(getCacheControl()).tag(newTag).build();
        }
      }
    }
    return Response.status(Status.BAD_REQUEST).build();
  }

  /**
   * A DELETE /contacts/person/{id} should delete a entry if exists
   *
   * @param id the unique identifier of a person
   * @return 204 if the request is successful, 404 if the id is not a key
   */
  @DELETE
  @Path("/person/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updatePerson(@PathParam("id") int id) {
    for (int i = 0; i < addressBook.getPersonList().size(); i++) {
      if (addressBook.getPersonList().get(i).getId() == id) {
        addressBook.getPersonList().remove(i);
        return Response.noContent().build();
      }
    }
    return Response.status(Status.NOT_FOUND).build();
  }

}
