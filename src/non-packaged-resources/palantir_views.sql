CREATE VIEW palantir.n3c_user AS
SELECT -- InCommon-federated
 	registration.email,
    registration.official_first_name,
    registration.official_last_name,
    registration.first_name,
    registration.last_name,
    registration.institution,
    registration.orcid_id,
    registration.expertise,
    registration.therapeutic_area,
    false as citizen_scientist,
    false as international,
    registration.created,
    registration.updated
   FROM n3c_admin.registration
  WHERE
  	registration.enclave
  AND
  	institution != 'NIH'
  AND
  	(official_institution is null OR official_institution != 'login.gov')
  AND
  	institution NOT IN (SELECT incommon from n3c_admin.registration_remap)
UNION
SELECT -- InCommon-federated, but name mismatch
 	registration.email,
    registration.official_first_name,
    registration.official_last_name,
    registration.first_name,
    registration.last_name,
    registration_remap.ror as institution,
    registration.orcid_id,
    registration.expertise,
    registration.therapeutic_area,
    false as citizen_scientist,
    false as international,
    registration.created,
    registration.updated
   FROM n3c_admin.registration, n3c_admin.registration_remap
  WHERE
  	registration.enclave
  AND
  	registration.institution = registration_remap.incommon
UNION
SELECT -- not InCommon-federated, but a ROR organization
 	registration.email,
    registration.official_first_name,
    registration.official_last_name,
    registration.first_name,
    registration.last_name,
    institutionname as institution,
    registration.orcid_id,
    registration.expertise,
    registration.therapeutic_area,
    false as citizen_scientist,
    false as international,
    registration.created,
    registration.updated
   FROM n3c_admin.registration, n3c_admin.registration_domain_remap, n3c_admin.site_master
  WHERE
  	registration.enclave
  AND
  	substring(email from '@(.*)$')=email_domain
  AND
  	registration_domain_remap.ror=site_master.institutionid
UNION
SELECT -- citizen-scientist
 	registration.email,
    registration.official_first_name,
    registration.official_last_name,
    registration.first_name,
    registration.last_name,
    null as institution,
    registration.orcid_id,
    registration.expertise,
    registration.therapeutic_area,
    true as citizen_scientist,
    false as international,
    registration.created,
    registration.updated
   FROM n3c_admin.registration, n3c_admin.citizen_master
  WHERE
  	registration.enclave
  AND
  	date_of_dua_signed is not null
  AND
  	registration.email=citizen_master.email_address
UNION
SELECT -- NIH personnel
 	registration.email,
    registration.official_first_name,
    registration.official_last_name,
    registration.first_name,
    registration.last_name,
    nih_ic.title as institution,
    registration.orcid_id,
    registration.expertise,
    registration.therapeutic_area,
    false as citizen_scientist,
    false as international,
    registration.created,
    registration.updated
   FROM n3c_admin.registration, nih_foa.nih_ic
where
	registration.enclave
and	institution='NIH'
and substring(official_full_name from '/([^)]+)') = nih_ic.ic;

CREATE VIEW palantir.n3c_organization AS
SELECT
 	organization.id,
    organization.name,
    '2020-07-20'::date as dua_signed,
    organization.wikipedia_url
   FROM ror.organization
  WHERE (organization.name IN ( 
  		SELECT registration.institution FROM n3c_admin.registration WHERE enclave and institution != ' NIH'
  		UNION
  		SELECT nih_ic.title FROM nih_foa.nih_ic where nih_ic.ic IN (select substring(official_full_name from '/([^)]+)') FROM n3c_admin.registration WHERE enclave and institution = 'NIH')
  		UNION
  		SELECT ror from n3c_admin.registration_remap
  		UNION
  		SELECT institutionname from n3c_admin.feedout,n3c_admin.registration_domain_remap where registration_domain_remap.ror=feedout.institutionid
  		));

CREATE VIEW palantir.n3c_organization2 AS
SELECT
 	organization.id,
    organization.name,
    duaexecuted as dua_signed,
    organization.wikipedia_url
   FROM ror.organization, n3c_admin.feedout
  WHERE (
  	organization.id = feedout.institutionid
  AND
  	organization.name IN ( 
  		SELECT registration.institution FROM n3c_admin.registration WHERE enclave and institution != ' NIH'
  		UNION
  		SELECT nih_ic.title FROM nih_foa.nih_ic where nih_ic.ic IN (select substring(official_full_name from '/([^)]+)') FROM n3c_admin.registration WHERE enclave and institution = 'NIH')
  		UNION
  		SELECT ror from n3c_admin.registration_remap
  		UNION
  		SELECT institutionname from n3c_admin.feedout,n3c_admin.registration_domain_remap where registration_domain_remap.ror=feedout.institutionid
  		));

CREATE VIEW palantir.citizen_scientist AS
SELECT * FROM n3c_admin.citizen_master;
