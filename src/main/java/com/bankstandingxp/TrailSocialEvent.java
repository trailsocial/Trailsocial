package com.bankstandingxp;

/**
 * Mirrors the fields returned by trailsocial.net's get-event.js Netlify
 * function that we actually care about. Deliberately does NOT declare an
 * imageDataUrl field - Gson just ignores that (often multi-MB) field on
 * the incoming JSON rather than materializing it into memory.
 */
class TrailSocialEvent
{
	String id;
	String title;
	String date;
	String endDate;
	String time;
	String location;
	String description;
}
