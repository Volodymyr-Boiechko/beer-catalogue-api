package com.haufe.beercatalogue.beer;

import com.haufe.beercatalogue.beer.dto.BeerRequest;
import com.haufe.beercatalogue.beer.dto.BeerResponse;
import com.haufe.beercatalogue.common.dto.PageResponse;
import com.haufe.beercatalogue.common.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "Beers", description = "Manage beers and search the catalogue")
public interface BeerApi {

    @Operation(summary = "Search beers", description = "Returns a paginated, filterable list of beers")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response")
    })
    PageResponse<BeerResponse> list(
        @ParameterObject BeerSearchCriteria criteria,
        @ParameterObject Pageable pageable
    );

    @Operation(summary = "Get beer by ID", description = "Returns a single beer by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Beer found"),
        @ApiResponse(responseCode = "404", description = "Beer not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    BeerResponse getById(@Parameter(description = "Beer ID") Long id);

    @Operation(summary = "Create beer", description = "Creates a new beer. Returns the created resource with a Location header. MANUFACTURER users may only create beers for their own manufacturer.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Beer created"),
        @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions or not the owner of the manufacturer",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "basicAuth")
    ResponseEntity<BeerResponse> create(BeerRequest request);

    @Operation(summary = "Update beer", description = "Updates an existing beer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Beer updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Beer not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "basicAuth")
    BeerResponse update(@Parameter(description = "Beer ID") Long id, BeerRequest request);

    @Operation(summary = "Delete beer", description = "Deletes a beer by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Beer deleted"),
        @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Beer not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "basicAuth")
    void delete(@Parameter(description = "Beer ID") Long id);
}
