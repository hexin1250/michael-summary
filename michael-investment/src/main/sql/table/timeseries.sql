create table timeseries (
	id Bigint NOT NULL AUTO_INCREMENT,
	variety varchar(10) NOT NULL,
	security varchar(20) NOT NULL,
	security_name varchar(20) NOT NULL,
	open numeric(10,4) NOT NULL,
	high numeric(10,4) NOT NULL,
	low numeric(10,4) NOT NULL,
	close numeric(10,4) NOT NULL,
	up_limit numeric(10,4),
	down_limit numeric(10,4),
	volume numeric(38,10) NOT NULL,
	open_interest numeric(38,10) NOT NULL,
	freq varchar(10) NOT NULL,
	trade_date varchar(10) NOT null,
	trade_ts timestamp NOT NULL,
	is_main_future varchar(1),
	PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
/*
insert into timeseries(security,security_name,open,high,low,close,up_limit,down_limit,volume,freq,trade_date,trade_ts,is_main_future) values('I',2,2,2,2,2,2,2,'1d','2020-01-01 00:00:00','f');
*/

/*
show indexes from timeseries;
*/
create index ts_s_index on timeseries (security);
create index ts_sf_index on timeseries (security, freq);
create index ts_stf_index on timeseries (security, trade_date, freq);
create index ts_vtf_index on timeseries (variety, trade_date, freq);
create index ts_vti_index on timeseries (variety, trade_date, is_main_future);
create index ts_vt_index on timeseries (variety, trade_date);
