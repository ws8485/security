package com.cws.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 예: ROLE_USER, ROLE_ADMIN
	@Column(nullable = false, unique = true)
	private String name;

	public Role() {
	}

	public Role(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	// 보통 id는 세터를 두지 않지만, 필요 시 주석 해제하여 사용하세요.
	// public void setId(Long id) {
//	     this.id = id;
	// }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}