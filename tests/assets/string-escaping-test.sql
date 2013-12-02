PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;
CREATE TABLE android_metadata (locale TEXT);
INSERT INTO "android_metadata" VALUES('en_US');
CREATE TABLE accounts (id integer primary key autoincrement, url text, blogName text, username text, password text, imagePlacement text, centerThumbnail boolean, fullSizeImage boolean, maxImageWidth text, maxImageWidthId integer, lastCommentId integer, runService boolean, blogId integer, location boolean default false, dotcom_username text, dotcom_password text, api_key text, api_blogid text, dotcomFlag boolean default false, wpVersion text, httpuser text, httppassword text, postFormats text default '', isScaledImage boolean default false, scaledImgWidth integer default 1024, homeURL text default '', blog_options text default '', isAdmin boolean default false, isHidden boolean default 0);
INSERT INTO accounts VALUES(350,'pouet.com','say: "pouet"','pouet','pouet','',0,0,'2000',5,NULL,0,6200060,'false',NULL,NULL,NULL,NULL,1,'','','AS3vw/BNTdI=','','false',1024,'pouet.com','','false',0);
COMMIT;
