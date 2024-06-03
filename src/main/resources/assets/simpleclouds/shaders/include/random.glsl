//https://stackoverflow.com/a/17479300

uint hash( uint x ) 
{
    x += ( x << 10u );
    x ^= ( x >>  6u );
    x += ( x <<  3u );
    x ^= ( x >> 11u );
    x += ( x << 15u );
    return x;
}

// Compound versions of the hashing algorithm.
uint hash_vec2( uvec2 v ) { return hash( v.x ^ hash(v.y)                         ); }
uint hash_vec3( uvec3 v ) { return hash( v.x ^ hash(v.y) ^ hash(v.z)             ); }
uint hash_vec4( uvec4 v ) { return hash( v.x ^ hash(v.y) ^ hash(v.z) ^ hash(v.w) ); }

// Construct a float with half-open range [0:1] using low 23 bits.
// All zeroes yields 0.0, all ones yields the next smallest representable value below 1.0.
float floatConstruct( uint m ) 
{
    const uint ieeeMantissa = 0x007FFFFFu; // binary32 mantissa bitmask
    const uint ieeeOne      = 0x3F800000u; // 1.0 in IEEE binary32

    m &= ieeeMantissa;                     // Keep only mantissa bits (fractional part)
    m |= ieeeOne;                          // Add fractional part to 1.0

    float  f = uintBitsToFloat( m );       // Range [1:2]
    return f - 1.0;                        // Range [0:1]
}

// Pseudo-random value in half-open range [0:1].
float random( float x ) { return floatConstruct(hash(floatBitsToUint(x))); }
float random_vec2( vec2  v ) { return floatConstruct(hash_vec2(floatBitsToUint(v))); }
float random_vec3( vec3  v ) { return floatConstruct(hash_vec3(floatBitsToUint(v))); }
float random_vec4( vec4  v ) { return floatConstruct(hash_vec4(floatBitsToUint(v))); }