package com.proseg.msvc_auth.entity;

import com.proseg.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_approbation_table")
public class User extends BaseEntity {

	@Id
	private UUID id;

	@NotBlank(message = "IdUsuario no puede estar vacio")
	@Column(name = "id_usuario", nullable = false, unique = true)
	private String keycloakUserId;

	@NotNull(message = "El status es requerido")
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private UserStatus status;

	@Column(name = "profile_image_object_name", length = 500)
	private String profileImageObjectName;

	public enum UserStatus {
		PENDING,
		APPROVED,
		REJECTED,
		INVITED
	}
}
