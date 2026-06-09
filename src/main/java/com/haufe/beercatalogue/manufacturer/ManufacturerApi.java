package com.haufe.beercatalogue.manufacturer;

import com.haufe.beercatalogue.common.dto.PageResponse;
import com.haufe.beercatalogue.common.exception.ApiError;
import com.haufe.beercatalogue.manufacturer.dto.ManufacturerRequest;
import com.haufe.beercatalogue.manufacturer.dto.ManufacturerResponse;
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

@Tag(name = "Manufacturers", description = "Manage beer manufacturers")
public interface ManufacturerApi {

    @Operation(summary = "List manufacturers", description = "Returns a paginated, filterable list of beer manufacturers")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response")
    })
    PageResponse<ManufacturerResponse> list(
        @ParameterObject ManufacturerSearchCriteria criteria,
        @ParameterObject Pageable pageable
    );

    @Operation(summary = "Get manufacturer by ID", description = "Returns a single manufacturer by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Manufacturer found"),
        @ApiResponse(responseCode = "404", description = "Manufacturer not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ManufacturerResponse getById(@Parameter(description = "Manufacturer ID") Long id);

    @Operation(summary = "Create manufacturer", description = "Creates a new manufacturer. Returns the created resource with a Location header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Manufacturer created"),
        @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "A manufacturer with this name and country already exists",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "basicAuth")
    ResponseEntity<ManufacturerResponse> create(ManufacturerRequest request);

    @Operation(summary = "Update manufacturer", description = "Updates an existing manufacturer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Manufacturer updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Manufacturer not found",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "A manufacturer with this name and country already exists",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "basicAuth")
    ManufacturerResponse update(@Parameter(description = "Manufacturer ID") Long id, ManufacturerRequest request);

    @Operation(summary = "Delete manufacturer", description = "Deletes a manufacturer by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Manufacturer deleted"),
        @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Manufacturer not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "basicAuth")
    void delete(@Parameter(description = "Manufacturer ID") Long id);
}
