create table backtest_run (
	run_id Bigint NOT NULL AUTO_INCREMENT,
	user_id varchar(100) NOT NULL,
	strategry_name varchar(64) NOT NULL,
	create_ts timestamp NOT NULL,
	PRIMARY KEY (run_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
