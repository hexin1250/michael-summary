create table USER_T(
	user_id bigint AUTO_INCREMENT not null,
	name varchar(255) not null,
	cash numeric(38,20) not null,
	create_ts timestamp not null,
	
	PRIMARY KEY (user_id), 
	KEY IDX_NAME (name) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table USER_SECURITY(
	user_id bigint not null,
	user_security_id bigint AUTO_INCREMENT not null,
	username varchar(255) not null,
	password varchar(255) not null,
	create_ts timestamp not null,
	
	PRIMARY KEY (user_security_id),
	CONSTRAINT FK_USER_SECURITY_USER_ID FOREIGN KEY (user_id) REFERENCES USER_T (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table PRODUCT_T(
	product_id bigint AUTO_INCREMENT not null,
	product_name varchar(255) not null,
	category varchar(255) not null,
	price numeric(38,20) not null,
	available_amount bigint not null,
	create_ts timestamp not null,
	
	PRIMARY KEY (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table ORDER_T(
	user_id bigint not null,
	order_id bigint AUTO_INCREMENT not null,
	product_id bigint not null,
	price numeric(38,20) not null,
	amount int not null,
	create_ts timestamp not null,
	
	PRIMARY KEY (order_id),
	CONSTRAINT FK_USER_ID FOREIGN KEY (user_id) REFERENCES USER_T (user_id),
	CONSTRAINT FK_PRODUCT_ID FOREIGN KEY (product_id) REFERENCES PRODUCT_T (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;